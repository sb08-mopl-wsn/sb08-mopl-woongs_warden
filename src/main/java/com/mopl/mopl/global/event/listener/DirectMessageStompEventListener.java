package com.mopl.mopl.global.event.listener;

import com.mopl.mopl.domain.dm.service.RoomPresenceManager;
import com.mopl.mopl.global.auth.details.MoplUserDetails;
import com.mopl.mopl.global.event.DirectMessageReadEvent;
import com.mopl.mopl.global.exception.BusinessException;
import com.mopl.mopl.global.exception.GlobalErrorCode;
import com.mopl.mopl.global.redis.dto.RedisPubMessage;
import com.mopl.mopl.global.redis.service.RedisPublisher;
import java.text.NumberFormat;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
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

  private static final String DM_DESTINATION_PATTERN = "/sub/conversations/{conversationId}/direct-messages";
  private static final String REDIS_PRESENCE_PREFIX = "room_presence:";

  private final AntPathMatcher pathMatcher = new AntPathMatcher();

  // 세션 연결 해제 시 정보를 찾기 위한 로컬 맵 (세션 ID는 서버 로컬)
  private final Map<String, String> sessionKeyMap = new ConcurrentHashMap<>();

  private final RedisTemplate<String, Object> redisTemplate;
  private final RedisPublisher redisPublisher;

  // 유저가 방에 있는지 판별 (Redis 글로벌 공유)
  @Override
  public boolean isUserInRoom(UUID userId, UUID conversationId) {
    String redisKey = REDIS_PRESENCE_PREFIX + conversationId.toString() + ":" + userId.toString();
    Object value = redisTemplate.opsForValue().get(redisKey);
    if (value instanceof Number num) {
      return num.intValue() > 0;
    } else if (value instanceof String str) {
      try {
        return Integer.parseInt(str) > 0;
      } catch (NumberFormatException e) {
        log.warn("Redis presence 값 파싱 실패 - key: {}, value: {}", redisKey, str);
        return false;
      }
    }
    return false;
  }

  // 클라이언트가 채팅방 화면에 들어왔을 때 (Subscribe)
  @EventListener
  public void handleSubscribe(SessionSubscribeEvent event) {

    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
    String destination = accessor.getDestination();

    if (destination == null || !pathMatcher.match(DM_DESTINATION_PATTERN, destination)) return;

    try {
      UUID conversationId = extractConversationId(destination);
      UUID userId = extractUserId(accessor);
      String sessionId = accessor.getSessionId();

      String redisKey = REDIS_PRESENCE_PREFIX + conversationId.toString() + ":" + userId.toString();

      // 세션 정보 로컬에 기록
      sessionKeyMap.put(sessionId, redisKey);
      
      // Redis 글로벌 카운트 증가
      Long count = redisTemplate.opsForValue().increment(redisKey);
      if (count != null && count == 1L) {
          redisTemplate.expire(redisKey, 24, TimeUnit.HOURS); // 예비용 만료 시간
      }

      log.debug("채팅방 입장 추적 완료 (Redis) - userId: {}, conversationId: {}", userId, conversationId);
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

  // 메시지 읽음 처리 이벤트 리스너
  @EventListener
  public void onDirectMessageRead(DirectMessageReadEvent event) {

    // 채팅방 주소 생성
    String destination = DM_DESTINATION_PATTERN.replace("{conversationId}", event.conversationId().toString());

    // 프론트엔드쪽에서 '새로운 메시지'인지 '읽음 처리 신호'인지 구별하도록 type을 달아서 전송
    Map<String, Object> readPayload = Map.of(
        "type", "READ_WATERMARK",
        "readerId", event.readerId(),
        "readAt", event.readAt()
    );

    // 로컬 템플릿 대신 RedisPublisher를 통해 모든 분산 서버로 발행
    redisPublisher.publishWs(new RedisPubMessage(null, destination, readPayload));
  }

  private void removeSession(String sessionId) {
    if (sessionId == null) return;
    String redisKey = sessionKeyMap.remove(sessionId);
    if (redisKey != null) {
      Long count = redisTemplate.opsForValue().decrement(redisKey);
      if (count != null && count <= 0) {
          redisTemplate.delete(redisKey);
      }
      log.debug("채팅방 퇴장 추적 완료 (Redis) - key: {}", redisKey);
    }
  }

  private UUID extractConversationId(String destination) {
    try {
      Map<String, String> variables = pathMatcher.extractUriTemplateVariables(DM_DESTINATION_PATTERN, destination);
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
