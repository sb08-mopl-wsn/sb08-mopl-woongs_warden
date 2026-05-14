package com.mopl.mopl.global.event.listener;

import com.mopl.mopl.domain.dm.service.RoomPresenceManager;
import com.mopl.mopl.global.auth.details.MoplUserDetails;
import com.mopl.mopl.global.exception.BusinessException;
import com.mopl.mopl.global.exception.GlobalErrorCode;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class DirectMessageStompEventListener implements RoomPresenceManager {

  private final AntPathMatcher pathMatcher = new AntPathMatcher();

  // 세션 연결 해제 시 정보를 찾기 위한 맵
  private final Map<String, String> sessionKeyMap = new ConcurrentHashMap<>();

  // 현재 방에 접속 중인 유저 판별용 맵
  private final Map<String, Integer> activeRoomMap = new ConcurrentHashMap<>();

  // 유저가 방에 있는지 판별
  @Override
  public boolean isUserInRoom(UUID userId, UUID conversationId) {
    String key = userId.toString() + ":" + conversationId.toString();
    return activeRoomMap.containsKey(key);
  }

  // 클라이언트가 채팅방 화면에 들어왔을 때 (Subscribe)
  @EventListener
  public void handleSubscribe(SessionSubscribeEvent event) {

    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
    String destination = accessor.getDestination();

    if (destination == null || !destination.matches("/sub/conversations/.+/direct-messages")) return;

    try {
      UUID conversationId = extractConversationId(destination);
      UUID userId = extractUserId(accessor);
      String sessionId = accessor.getSessionId();

      String key = userId.toString() + ":" + conversationId.toString();

      // 세션 정보 기록 (멀티 탭 지원을 위해 count 증가)
      sessionKeyMap.put(sessionId, key);
      activeRoomMap.merge(key, 1, Integer::sum);

      log.debug("채팅방 입장 추적 완료 - userId: {}, conversationId: {}", userId, conversationId);
    } catch (Exception e) {
      log.error("[Subscribe ERROR] DM 채팅방 구독 추적 실패", e);
    }
  }

  // 클라이언트가 뒤로가기를 누르거나 다른 방으로 갔을 때 (Unsubscribe)
  @EventListener
  public void handleUnSubscribe(SessionUnsubscribeEvent event) {
    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
    removeSession(accessor.getSessionId());
  }

  // 클라이언트가 브라우저 창 자체를 끄거나 인터넷이 끊겼을 때 (Disconnect)
  @EventListener
  public void handleLeave(SessionDisconnectEvent event) {
    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
    removeSession(accessor.getSessionId());
  }

  private void removeSession(String sessionId) {
    String key = sessionKeyMap.remove(sessionId);
    if (key != null) {
      // 브라우저 탭이 여러개일수 있으므로 count를 1 깎고, 0이되면 맵에서 완전히 지움
      activeRoomMap.computeIfPresent(key, (k, count) -> count > 1 ? count - 1 : null);
      log.debug("채팅방 퇴장 추적 완료 - key: {}", key);
    }
  }

  private UUID extractConversationId(String destination) {
    String pattern = "/sub/conversation/{conversationId}/direct-messages";
    try {
      Map<String, String> variables = pathMatcher.extractUriTemplateVariables(pattern, destination);
      return UUID.fromString(variables.get("conversationId"));
    } catch (Exception e) {
      throw new BusinessException(GlobalErrorCode.INVALID_INPUT, "잘못된 웹소켓 경로입니다.");
    }
  }

  private UUID extractUserId(StompHeaderAccessor accessor) {
    UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) accessor.getUser();
    if (auth == null || auth.getPrincipal() == null) {
      throw new BusinessException(GlobalErrorCode.UNAUTHORIZED, "웹소켓 인증 정보가 없습니다.");
    }
    MoplUserDetails userDetails = (MoplUserDetails) auth.getPrincipal();
    return userDetails.getUserDto().id();
  }
}
