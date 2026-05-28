package com.mopl.mopl.global.event.listener;

import com.mopl.mopl.domain.watchingSession.service.WatchingSessionService;
import com.mopl.mopl.global.auth.details.MoplUserDetails;
import com.mopl.mopl.global.component.WebSocketSessionRegistry;
import com.mopl.mopl.global.event.UserLogoutEvent;
import com.mopl.mopl.global.exception.BusinessException;
import com.mopl.mopl.global.exception.GlobalErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.time.Duration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * STOMP 프로토콜 이벤트를 수신하여 사용자의 시청 세션(입장/퇴장)을 동기화하고 관리한다.
 * 분산 환경으로의 확장성을 고려하여 유저 단위 락을 통해 동시성 문제를 제어한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WatchingSessionStompEventListener {

    //service
    private final WatchingSessionService watchingSessionService;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final WebSocketSessionRegistry registry;

    private final StringRedisTemplate redisTemplate;

    // Redis Key Prefix 규칙 정의
    private static final String KEY_USER_SESSIONS = "ws:user:%s:sessions";        // Set 구조 (유저별 세션 리스트)
    private static final String KEY_SESSION_CONTENTS = "ws:session:%s:contents"; // Set 구조 (세션별 시청 룸 리스트)
    private static final String KEY_SUB_MAP = "ws:sub:%s:%s";                     // String 구조 (세션+구독ID별 콘텐트ID)
    private static final String KEY_SESSION_SUBS = "ws:session:%s:subs";         // Set 구조 (세션별 구독 ID 리스트 - Keys() 대체)
    private static final String KEY_WATCH_LOCK = "lock:watch:%s:%s";         // 중복 join 방지 분산 락

    // Redis TTL 3시간 설정
    private static final Duration SESSION_TTL = Duration.ofHours(3);

    /**
     * 웹 소켓 최초 연결 시 호출. 유저와 세션을 매핑하여 상태 관리를 준비한다.
     */
    @EventListener
    public void handleConnect(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        try {
            String sessionId = accessor.getSessionId();
            UUID userId = extractUserId(accessor);

            String userSessionKey = String.format(KEY_USER_SESSIONS, userId);
            // Redis Set에 유저-세션 매핑 데이터 추가 (싱글 스레드 연산 원자적 연산)
            redisTemplate.opsForSet().add(userSessionKey, sessionId);
            redisTemplate.expire(userSessionKey, SESSION_TTL);
        } catch (Exception e) {
            // 인증되지 않은 연결은 무시한다.
        }
    }

    /**
     * /sub/contents/{contentId}/watch 경로 구독 시 호출.
     * 분산 환경에서 두 서버가 동시에 같은 유저의 구독을 처리할 때 중복 join()을 막기 위해
     * Redis 분산 락을 먼저 획득한 후 Redis 상태를 업데이트한다.
     * 다른 탭/세션에서 이미 시청 중인 경우 join을 건너뛴다.
     *
     * @param event STOMP 구독 이벤트
     */
    @EventListener
    public void handleSubscribe(SessionSubscribeEvent event) {

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = accessor.getDestination();

        if (destination == null || !destination.matches("/sub/contents/.+/watch")) return;

        try {
            String sessionId = accessor.getSessionId();
            String subscriptionId = accessor.getSubscriptionId();
            UUID contentId = extractContentId(destination);
            UUID userId = extractUserId(accessor);

            // 분산 락으로 중복 join 방지
            String lockKey = String.format(KEY_WATCH_LOCK, userId, contentId);
            Boolean acquired = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, "1", Duration.ofSeconds(5));

            if (!Boolean.TRUE.equals(acquired)) return;

            try {
                boolean isAlreadyWatching = isUserWatching(userId, contentId);

                String sessionContentKey = String.format(KEY_SESSION_CONTENTS, sessionId);
                redisTemplate.opsForSet().add(sessionContentKey, contentId.toString());
                redisTemplate.expire(sessionContentKey, SESSION_TTL);

                String subMapKey = String.format(KEY_SUB_MAP, sessionId, subscriptionId);
                redisTemplate.opsForValue().set(subMapKey, contentId.toString(), SESSION_TTL);

                redisTemplate.opsForSet().add(String.format(KEY_USER_SESSIONS, userId), sessionId);

                // keys() 성능 저하를 막기 위해 세션별 구독 식별자를 전용 셋으로 관리한다.
                String sessionSubKey = String.format(KEY_SESSION_SUBS, sessionId);
                redisTemplate.opsForSet().add(sessionSubKey, subscriptionId);
                redisTemplate.expire(sessionSubKey, SESSION_TTL);


                if (!isAlreadyWatching) {
                    watchingSessionService.join(contentId, userId);
                }
            } finally {
                redisTemplate.delete(lockKey);
            }

        } catch (BusinessException e) {
            log.warn("[Subscribe FAIL] destination={}, code={}, message={}",
                    destination, e.getErrorCode().getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("[Subscribe ERROR] destination={}", destination, e);
        }
    }

    /**
     * 사용자가 명시적으로 구독을 취소할 때 호출된다. (다른 페이지로 이동)
     * 구독 관련 Redis 키를 정리하고, 다른 세션/탭에서도 시청 중인지 확인 후
     * 완전히 퇴장한 경우에만 leave()를 호출한다.
     *
     * @param event STOMP 구독 취소 이벤트
     */
    @EventListener
    public void handleUnSubscribe(SessionUnsubscribeEvent event) {

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        String subscriptionId = accessor.getSubscriptionId();

        try {
            String subKey = String.format(KEY_SUB_MAP, sessionId, subscriptionId);
            String contentIdStr = redisTemplate.opsForValue().get(subKey);
            if (contentIdStr == null) return;

            UUID contentId = UUID.fromString(contentIdStr);

            redisTemplate.delete(subKey);
            redisTemplate.opsForSet().remove(String.format(KEY_SESSION_SUBS, sessionId), subscriptionId);

            String sessionContentKey = String.format(KEY_SESSION_CONTENTS, sessionId);
            redisTemplate.opsForSet().remove(sessionContentKey, contentIdStr);

            Long remainRoomCount = redisTemplate.opsForSet().size(sessionContentKey);
            if (remainRoomCount != null && remainRoomCount == 0) {
                redisTemplate.delete(sessionContentKey);
            }

            UUID userId = extractUserId(accessor);

            if (!isUserWatching(userId, contentId)) {
                watchingSessionService.leave(contentId, userId);
            }
        } catch (BusinessException e) {
            log.warn("[Unsubscribe FAIL] sessionId={}, code={}, message={}",
                    sessionId, e.getErrorCode().getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("[Unsubscribe ERROR] sessionId={}", sessionId, e);
        }
    }

    /**
     * 사용자의 WebSocket 연결이 끊어질 때 호출된다. (브라우저 종료, 네트워크 단절)
     * 해당 세션의 모든 Redis 키를 정리하고, 유저의 다른 세션이 없을 경우 유저 키도 삭제한다.
     * 각 콘텐츠에 대해 다른 세션에서 시청 중인지를 확인 후 완전히 퇴장한 경우에만 leave()를 호출한다.
     *
     * @param event STOMP 연결 종료 이벤트
     */
    @EventListener
    public void handleLeave(SessionDisconnectEvent event) {

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        try {
            String sessionContentKey = String.format(KEY_SESSION_CONTENTS, sessionId);
            Set<String> contentIdsStr = redisTemplate.opsForSet().members(sessionContentKey);

            redisTemplate.delete(sessionContentKey);

            Set<String> subIds = redisTemplate.opsForSet().members(String.format(KEY_SESSION_SUBS, sessionId));
            if (subIds != null && !subIds.isEmpty()) {
                for (String subId : subIds) {
                    redisTemplate.delete(String.format(KEY_SUB_MAP, sessionId, subId));
                }
                redisTemplate.delete(String.format(KEY_SESSION_SUBS, sessionId));
            }

            UUID userId = null;
            try {
                userId = extractUserId(accessor);
            } catch (Exception ignored) {
            }
            if (userId == null) return;

            String userSessionKey = String.format(KEY_USER_SESSIONS, userId);
            redisTemplate.opsForSet().remove(userSessionKey, sessionId);

            Long remainSessionCount = redisTemplate.opsForSet().size(userSessionKey);
            if (remainSessionCount != null && remainSessionCount == 0) {
                redisTemplate.delete(userSessionKey);
            }

            if (contentIdsStr != null) {
                for (String idStr : contentIdsStr) {
                    UUID contentId = UUID.fromString(idStr);
                    if (!isUserWatching(userId, contentId)) {
                        try {
                            watchingSessionService.leave(contentId, userId);
                        } catch (Exception e) {
                            log.error("[Disconnect ERROR] DB 접속 에러", e);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("[Disconnect ERROR] sessionId = {}", sessionId, e);
        }
    }

    /**
     * 로그아웃 이벤트 수신 시 호출.
     * 유저의 모든 세션에 대한 Redis 키를 일괄 정리하고 Websocket 연결을 강제 종료한다.
     * uniqueContentIdsToLeave를 HashSet으로 관리하여 멀티탭 환경에서 levae()가 중복 호출되는 것을 방지한다.
     *
     * @param event 로그아웃 이벤트 수신 이벤트
     */
    @EventListener
    public void handleUserLogout(UserLogoutEvent event) {
        UUID userId = event.userId();
        Set<UUID> uniqueContentIdsToLeave = new HashSet<>();

        String userSessionKey = String.format(KEY_USER_SESSIONS, userId);
        Set<String> sessionIds = redisTemplate.opsForSet().members(userSessionKey);
        redisTemplate.delete(userSessionKey);

        if (sessionIds != null) {
            for (String sessionId : sessionIds) {
                String sessionContentKey = String.format(KEY_SESSION_CONTENTS, sessionId);
                Set<String> contentIdsStr = redisTemplate.opsForSet().members(sessionContentKey);
                redisTemplate.delete(sessionContentKey);

                if (contentIdsStr != null) {
                    for (String idStr : contentIdsStr) {
                        uniqueContentIdsToLeave.add(UUID.fromString(idStr));
                    }
                }

                Set<String> subIds = redisTemplate.opsForSet().members(String.format(KEY_SESSION_SUBS, sessionId));
                if (subIds != null && !subIds.isEmpty()) {
                    for (String subId : subIds) {
                        redisTemplate.delete(String.format(KEY_SUB_MAP, sessionId, subId));
                    }
                    redisTemplate.delete(String.format(KEY_SESSION_SUBS, sessionId));
                }

                try {
                    WebSocketSession session = registry.getSession(sessionId);
                    if (session != null && session.isOpen()) session.close();
                } catch (Exception e) {
                    log.error("[UserLogout ERROR] 웹소켓 종료 중 문제 발생", e);
                }
            }
        }

        for (UUID contentId : uniqueContentIdsToLeave) {
            try {
                watchingSessionService.leave(contentId, userId);
            } catch (Exception e) {
                log.error("[UserLogout ERROR] DB 접속 에러", e);
            }
        }
        log.info("[Logout] Redis 세션 및 DB 일괄 정리 완료. userId={}", userId);
    }

    /**
     * 구독 대상 경로에서 콘텐츠 ID를 추출한다.
     * 경로 형식: /sub/contents/{contentId}/watch
     */
    private UUID extractContentId(String destination) {

        String pattern = "/sub/contents/{contentId}/watch";

        if (!pathMatcher.match(pattern, destination)) {
            log.warn("[Subscribe] 잘못된 구독 경로: destination={}", destination);
            throw new BusinessException(GlobalErrorCode.INVALID_INPUT);
        }

        try {
            Map<String, String> variables = pathMatcher.extractUriTemplateVariables(pattern, destination);
            return UUID.fromString(variables.get("contentId")); // UUID 형식 오류
        } catch (IllegalArgumentException e) {
            log.warn("[Subscribe] contentId UUID 형식 오류: destination={}", destination);
            throw new BusinessException(GlobalErrorCode.INVALID_INPUT);
        }
    }

    // STOMP 헤더의 인증 정보에서 사용자 ID를 추출한다.
    private UUID extractUserId(StompHeaderAccessor accessor) {
        UsernamePasswordAuthenticationToken auth =
                (UsernamePasswordAuthenticationToken) accessor.getUser();

        MoplUserDetails userDetails = (MoplUserDetails) auth.getPrincipal();

        return userDetails.getUserDto().id();
    }

    /**
     * 유저가 특정 콘텐츠를 시청 중인지 확인한다.
     * 유저의 모든 세션을 순회하여 해당 contentId가 하나라도 있으면 true를 반환한다.
     * 멀티탭 환경을 고려하여 세션 단위가 아닌 유저 단위로 판단한다.
     */
    private boolean isUserWatching(UUID userId, UUID contentId) {
        String userSessionKey = String.format(KEY_USER_SESSIONS, userId);
        Set<String> userSessions = redisTemplate.opsForSet().members(userSessionKey);
        if (userSessions == null || userSessions.isEmpty()) return false;

        for (String session : userSessions) {
            String sessionContentKey = String.format(KEY_SESSION_CONTENTS, session);
            Boolean isMember = redisTemplate.opsForSet().isMember(sessionContentKey, contentId.toString());
            // NPE 방지를 위해 null이 들어와도 안전하게 false를 반환할 수 있게 설계
            if (Boolean.TRUE.equals(isMember)) {
                return true;
            }
        }
        return false;
    }
}
