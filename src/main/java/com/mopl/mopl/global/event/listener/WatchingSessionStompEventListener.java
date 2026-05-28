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
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * STOMP 프로토콜 이벤트를 수신하여 사용자의 시청 세션(입장/퇴장)을 동기화하고 관리한다.
 * 분산 환경으로의 확장성을 고려하여 유저 단위 락을 통해 동시성 문제를 제어한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WatchingSessionStompEventListener {

    private final WatchingSessionService watchingSessionService;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final WebSocketSessionRegistry registry;

    // 연결 종료 이벤트 처리 용도
    private final Map<String, Set<UUID>> sessionContentMap = new ConcurrentHashMap<>();
    // 구독 취소 이벤트 처리 용도
    private final Map<String, UUID> subscriptionContentMap = new ConcurrentHashMap<>();
    // 로그아웃을 위한 역방향 맵
    private final Map<UUID, Set<String>> userSessionMap = new ConcurrentHashMap<>();

    // 유저 별 고유 락 관리 맵
    private final Map<UUID, Object> userLocks = new ConcurrentHashMap<>();

    private Object getUserLock(UUID userId) {
        return userLocks.computeIfAbsent(userId, k -> new Object());
    }

    /**
     * 웹 소켓 최초 연결 시 호출. 유저와 세션을 매핑하여 상태 관리를 준비한다.
     */
    @EventListener
    public void handleConnect(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        try {
            String sessionId = accessor.getSessionId();
            UUID userId = extractUserId(accessor);

            synchronized (getUserLock(userId)) {
                userSessionMap
                        .computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet())
                        .add(sessionId);
            }
        } catch (Exception e) {
            // 인증되지 않은 연결은 무시한다.
        }
    }

    /**
     * 사용자가 특정 콘텐츠의 시청 경로를 구독할 때 호출된다.
     * 구독 경로가 유효한 경우 세션 정보를 기록하고 서비스의 입장(join) 로직을 실행한다.
     * 첫 입장 시에만 서비스의 join 로직을 수행한다.
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

            synchronized (getUserLock(userId)) {
                boolean isAlreadyWatching = isUserWatching(userId, contentId);

                sessionContentMap
                        .computeIfAbsent(sessionId, k -> ConcurrentHashMap.newKeySet())
                        .add(contentId);

                subscriptionContentMap.put(sessionId + ":" + subscriptionId, contentId);

                userSessionMap
                        .computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet())
                        .add(sessionId);

                if (!isAlreadyWatching) {
                    watchingSessionService.join(contentId, userId);
                }
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
     * 기록된 구독 정보를 바탕으로 서비스의 퇴장(leave) 로직을 실행한다.
     *
     * @param event STOMP 구독 취소 이벤트
     */
    @EventListener
    public void handleUnSubscribe(SessionUnsubscribeEvent event) {

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        String subscriptionId = accessor.getSubscriptionId();

        try {
            String key = sessionId + ":" + subscriptionId;
            UUID contentId = subscriptionContentMap.remove(key);
            if (contentId == null) return;

            UUID userId = extractUserId(accessor);
            boolean shouldLeave = false;

            synchronized (getUserLock(userId)) {
                boolean isSessionStillSubscribed = subscriptionContentMap.entrySet().stream()
                        .anyMatch(entry -> entry.getKey().startsWith(sessionId + ":") && entry.getValue().equals(contentId));

                if (isSessionStillSubscribed) {
                    log.debug("[Unsubscribe] 빠른 재접속 감지합니다. sessionId={}, contentId={}", sessionId, contentId);
                    return;
                }

                Set<UUID> contentIds = sessionContentMap.get(sessionId);
                if (contentIds != null) {
                    contentIds.remove(contentId);
                    if (contentIds.isEmpty()) {
                        sessionContentMap.remove(sessionId);
                    }
                }

                boolean isUserStillWatching = isUserWatching(userId, contentId);

                if (!isUserStillWatching) {
                    shouldLeave = true;
                }
            }

            if (shouldLeave) {
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
     * 남겨진 세션 정보를 정리하고, 최종적으로 leave 로직을 수행한다.
     *
     * @param event STOMP 연결 종료 이벤트
     */
    @EventListener
    public void handleLeave(SessionDisconnectEvent event) {

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        try {
            Set<UUID> contentIds = sessionContentMap.remove(sessionId);
            subscriptionContentMap.keySet()
                    .removeIf(key -> key.startsWith(sessionId + ":"));
            UUID userId = null;
            try { userId = extractUserId(accessor); } catch (Exception ignored) {}
            if (userId == null) return;

            Set<UUID> contentIdsToLeave = new HashSet<>();

            synchronized (getUserLock(userId)) {
                removeSessionFromUserMap(userId, sessionId);

                if (contentIds != null) {
                    for (UUID contentId : contentIds) {
                        if (!isUserWatching(userId, contentId)) {
                            contentIdsToLeave.add(contentId);
                        }
                    }
                }
            }
            // DB 작업은 락 밖에 안전하게 수행한다.
            for (UUID contentId : contentIdsToLeave) {
                try {
                    watchingSessionService.leave(contentId, userId);
                } catch (Exception e) {
                    log.error("[Disconnect ERROR] DB 접속 에러", e);
                }
            }
        } catch (Exception e) {
            log.error("[Disconnect ERROR] sessionId = {}", sessionId, e);
        }
    }

    /**
     * 로그아웃 이벤트 수신 시 유저의 모든 웹소켓 연결을 강제 종료하고 상태를 정리한다.
     * @param event 로그아웃 이벤트 수신 이벤트
     */
    @EventListener
    public void handleUserLogout(UserLogoutEvent event) {
        UUID userId = event.userId();
        Set<UUID> uniqueContentIdsToLeave = new HashSet<>();

        synchronized (getUserLock(userId)) {
            Set<String> sessionIds = userSessionMap.remove(userId);
            if (sessionIds == null) return;

            for (String sessionId : sessionIds) {
                Set<UUID> contentIds = sessionContentMap.remove(sessionId);
                if (contentIds != null) uniqueContentIdsToLeave.addAll(contentIds);
                subscriptionContentMap.keySet().removeIf(key -> key.startsWith(sessionId + ":"));

                try {
                    WebSocketSession session = registry.getSession(sessionId);
                    if (session != null && session.isOpen()) session.close();
                } catch (Exception e) {
                    log.error("[Logout ERROR]", e);
                }
            }
            userLocks.remove(userId);
        }

        // 락 밖에서 순차적으로 퇴장 처리한다.
        for (UUID contentId : uniqueContentIdsToLeave) {
            try {
                watchingSessionService.leave(contentId, userId);
            } catch (Exception e) {
                log.error("[Logout ERROR] DB 접속 에러", e);
            }
        }
        log.info("[Logout] 세션 및 DB 정리 완료. userId={}", userId);
    }

    // 구독 대상 경로에서 콘텐츠 ID를 추출한다.
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

    // 유저별 세션 ID 목록에서 특정 세션을 제거한다.
    private void removeSessionFromUserMap(UUID userId, String sessionId) {
        userSessionMap.computeIfPresent(userId, (k, sessionIds) -> {
            sessionIds.remove(sessionId);
            return sessionIds.isEmpty() ? null : sessionIds;
        });
    }

    // 특정 유저가 콘텐츠를 시청 중인지를 확인한다.
    private boolean isUserWatching(UUID userId, UUID contentId) {
        Set<String> userSessions = userSessionMap.get(userId);
        if (userSessions == null || userSessions.isEmpty()) return false;

        for (String userSession : userSessions) {
            Set<UUID> contentIds = sessionContentMap.get(userSession);
            if (contentIds != null && contentIds.contains(contentId)) {
                return true;
            }
        }
        return false;
    }
}
