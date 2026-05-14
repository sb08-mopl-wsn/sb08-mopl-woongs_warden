package com.mopl.mopl.global.event.listener;

import com.mopl.mopl.domain.watchingSession.service.WatchingSessionService;
import com.mopl.mopl.global.auth.details.MoplUserDetails;
import com.mopl.mopl.global.exception.BusinessException;
import com.mopl.mopl.global.exception.GlobalErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * STOMP 프로토콜의 연결 상태 변화를 감지하여 시청 세션을 관리하는 리스너이다.
 * 사용자의 구독, 구독 취소, 연결 종료 이벤트를 추적하여 입장/퇴장 로직을 호출한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WatchingSessionStompEventListener {

    private final WatchingSessionService watchingSessionService;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    // 연결 종료 이벤트 처리 용도
    private final Map<String, Set<UUID>> sessionContentMap = new ConcurrentHashMap<>();
    // 구독 취소 이벤트 처리 용도
    private final Map<String, UUID> subscriptionContentMap = new ConcurrentHashMap<>();

    /**
     * 사용자가 특정 콘텐츠의 시청 경로를 구독할 때 호출된다.
     * 구독 경로가 유효한 경우 세션 정보를 기록하고 서비스의 입장(join) 로직을 실행한다.
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

            sessionContentMap
                    .computeIfAbsent(sessionId, k -> ConcurrentHashMap.newKeySet())
                    .add(contentId);

            subscriptionContentMap.put(sessionId + ":" + subscriptionId, contentId);

            watchingSessionService.join(contentId, userId);
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

            Set<UUID> contentIds = sessionContentMap.get(sessionId);
            if (contentIds != null) {
                contentIds.remove(contentId);
                if (contentIds.isEmpty()) sessionContentMap.remove(sessionId);
            }

            UUID userId = extractUserId(accessor);

            watchingSessionService.leave(contentId, userId);
        } catch (BusinessException e) {
            log.warn("[Unsubscribe FAIL] sessionId={}, code={}, message={}",
                    sessionId, e.getErrorCode().getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("[Unsubscribe ERROR] sessionId={}", sessionId, e);
        }
    }

    /**
     * 사용자의 WebSocket 연결이 끊어질 때 호출된다. (브라우저 종료, 네트워크 단절)
     * 세션 맵에 정보를 확인하여 처리되지 않은 퇴장 로직을 마무리한다.
     *
     * @param event STOMP 연결 종료 이벤트
     */
    @EventListener
    public void handleLeave(SessionDisconnectEvent event) {

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        try {
            Set<UUID> contentIds = sessionContentMap.remove(sessionId);
            if (contentIds == null || contentIds.isEmpty()) return;

            UUID userId = extractUserId(accessor);

            for (UUID contentId : contentIds) {
                try {
                    watchingSessionService.leave(contentId, userId);
                } catch (Exception e) {
                    log.error("[Disconnect ERROR] sessionId={}, contentId={}", sessionId, contentId, e);
                }
            }
        } catch (Exception e) {
            // 단순 로깅
            // 연결 종료 시점이라 사용자에게 전달 불가하다.
            log.error("[Disconnect ERROR] sessionId = {}", sessionId, e);
        }
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
}
