package com.mopl.mopl.global.event.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

import com.mopl.mopl.domain.user.dto.UserDto;
import com.mopl.mopl.domain.user.entity.Role;
import com.mopl.mopl.global.auth.details.MoplUserDetails;
import com.mopl.mopl.global.event.DirectMessageReadEvent;
import com.mopl.mopl.global.redis.dto.RedisPubMessage;
import com.mopl.mopl.global.redis.service.RedisPublisher;
import java.security.Principal;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

@Slf4j
@ExtendWith(MockitoExtension.class)
class DirectMessageStompEventListenerTest {

  @InjectMocks
  private DirectMessageStompEventListener listener;

  @Mock
  private RedisTemplate<String, Object> redisTemplate;

  @Mock
  private ValueOperations<String, Object> valueOperations;

  @Mock
  private RedisPublisher redisPublisher;

  private UUID userId;
  private UUID conversationId;
  private String sessionId;
  private String validDestination;
  private String redisKey;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    conversationId = UUID.randomUUID();
    sessionId = "session-123";
    validDestination = "/sub/conversations/" + conversationId + "/direct-messages";
    redisKey = "room_presence:" + conversationId.toString() + ":" + userId.toString();
  }

  // 가짜 인증 토큰 생성 헬퍼 메서드
  private UsernamePasswordAuthenticationToken createAuthToken(UUID userId) {
    UserDto userDto = new UserDto(userId, Instant.now(), "test@test.com", "테스트", null, Role.USER, false, false);
    MoplUserDetails userDetails = new MoplUserDetails(userDto, "password", Collections.emptyMap());
    return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
  }

  // STOMP Message 구조 생성 헬퍼 메서드
  private Message<byte[]> createMessage(StompCommand command, String destination, String sessionId, boolean withUser) {
    StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
    if (destination != null) accessor.setDestination(destination);
    accessor.setSessionId(sessionId);
    if (withUser) accessor.setUser(createAuthToken(userId));
    return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
  }

  @Test
  @DisplayName("방에 접속중인지 확인 (Redis에 count > 0 이면 true 반환)")
  void isUserInRoom_ReturnsTrue() {
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.get(redisKey)).thenReturn(1);

    boolean result = listener.isUserInRoom(userId, conversationId);

    assertThat(result).isTrue();
  }

  @Test
  @DisplayName("방에 접속중인지 확인 (Redis 값이 없거나 <= 0 이면 false 반환)")
  void isUserInRoom_ReturnsFalse() {
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.get(redisKey)).thenReturn(null);

    boolean result = listener.isUserInRoom(userId, conversationId);

    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("정상 경로 구독 시 Redis의 방 접속 카운트를 1 증가시킨다.")
  void handleSubscribe_Success() {
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.increment(redisKey)).thenReturn(1L);

    Message<byte[]> message = createMessage(StompCommand.SUBSCRIBE, validDestination, sessionId, true);
    SessionSubscribeEvent event = new SessionSubscribeEvent(this, message, mock(Principal.class));

    listener.handleSubscribe(event);

    verify(valueOperations).increment(redisKey);
    verify(redisTemplate).expire(redisKey, 24, TimeUnit.HOURS);
  }

  @Test
  @DisplayName("구독 취소 시 Redis의 방 접속 카운트를 1 감소시키고 0이하가 되면 삭제한다.")
  void handleUnSubscribe_Success() {
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.increment(redisKey)).thenReturn(1L);

    // 입장
    Message<byte[]> subMessage = createMessage(StompCommand.SUBSCRIBE, validDestination, sessionId, true);
    listener.handleSubscribe(new SessionSubscribeEvent(this, subMessage, mock(Principal.class)));

    // 퇴장 준비
    when(valueOperations.decrement(redisKey)).thenReturn(0L);

    // 퇴장
    Message<byte[]> unMessage = createMessage(StompCommand.UNSUBSCRIBE, null, sessionId, false);
    listener.handleUnSubscribe(new SessionUnsubscribeEvent(this, unMessage, mock(Principal.class)));

    verify(valueOperations).decrement(redisKey);
    verify(redisTemplate).delete(redisKey);
  }

  @Test
  @DisplayName("지정되지 않은 경로를 구독하면 무시된다.")
  void handleSubscribe_InvalidDestination_Ignored() {
    Message<byte[]> message = createMessage(StompCommand.SUBSCRIBE, "/sub/wrong/path", sessionId, true);
    SessionSubscribeEvent event = new SessionSubscribeEvent(this, message, mock(Principal.class));

    listener.handleSubscribe(event);

    verify(redisTemplate, never()).opsForValue();
  }

  @Test
  @DisplayName("구독 시 인증 정보(Principal)가 없으면 예외를 삼키고 넘어간다.")
  void handleSubscribe_NoAuth_HandledGracefully() {
    Message<byte[]> message = createMessage(StompCommand.SUBSCRIBE, validDestination, sessionId, false);
    SessionSubscribeEvent event = new SessionSubscribeEvent(this, message, null);

    assertDoesNotThrow(() -> listener.handleSubscribe(event));
    verify(redisTemplate, never()).opsForValue();
  }

  @Test
  @SuppressWarnings("unchecked")
  @DisplayName("DM 읽음 처리 이벤트 - RedisPublisher를 통해 읽음 Watermark 신호를 모든 서버로 발행한다.")
  void onDirectMessageRead_BroadcastsWatermarkViaRedis() {
    Instant now = Instant.now();
    DirectMessageReadEvent event = DirectMessageReadEvent.of(conversationId, userId, now);
    String expectedDestination = "/sub/conversations/" + conversationId + "/direct-messages";

    listener.onDirectMessageRead(event);

    ArgumentCaptor<RedisPubMessage> captor = ArgumentCaptor.forClass(RedisPubMessage.class);
    verify(redisPublisher).publishWs(captor.capture());

    RedisPubMessage message = captor.getValue();
    assertThat(message.eventName()).isEqualTo(expectedDestination);

    Map<String, Object> payload = (Map<String, Object>) message.data();
    assertThat(payload.get("type")).isEqualTo("READ_WATERMARK");
    assertThat(payload.get("readerId")).isEqualTo(userId);
    assertThat(payload.get("readAt")).isEqualTo(now);
  }

  @Test
  @DisplayName("다중 탭 시나리오 - 같은 사용자가 여러 세션으로 접속하면 카운트가 증가하고, 모두 퇴장해야 0이 된다.")
  void handleSubscribe_MultipleSessions_CountsCorrectly() {
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.increment(redisKey)).thenReturn(1L, 2L);

    String sessionId1 = "session-1";
    String sessionId2 = "session-2";

    // 첫 번째 탭 입장
    Message<byte[]> msg1 = createMessage(StompCommand.SUBSCRIBE, validDestination, sessionId1, true);
    listener.handleSubscribe(new SessionSubscribeEvent(this, msg1, mock(Principal.class)));

    // 두 번째 탭 입장
    Message<byte[]> msg2 = createMessage(StompCommand.SUBSCRIBE, validDestination, sessionId2, true);
    listener.handleSubscribe(new SessionSubscribeEvent(this, msg2, mock(Principal.class)));

    verify(valueOperations, times(2)).increment(redisKey);
  }

  @Test
  @DisplayName("브라우저 종료 시 handleLeave가 Redis 카운트를 감소시킨다.")
  void handleLeave_DecrementsRedisCount() {
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.increment(redisKey)).thenReturn(1L);
    when(valueOperations.decrement(redisKey)).thenReturn(0L);

    // 입장
    Message<byte[]> subMessage = createMessage(StompCommand.SUBSCRIBE, validDestination, sessionId, true);
    listener.handleSubscribe(new SessionSubscribeEvent(this, subMessage, mock(Principal.class)));

    // 브라우저 종료
    Message<byte[]> disconnectMessage = createMessage(StompCommand.DISCONNECT, null, sessionId, false);
    listener.handleLeave(new SessionDisconnectEvent(this, disconnectMessage, sessionId, null));

    verify(valueOperations).decrement(redisKey);
    verify(redisTemplate).delete(redisKey);
  }

  @Test
  @DisplayName("브라우저 종료 시 Redis 카운트가 1 이상이면 키를 삭제하지 않는다.")
  void handleLeave_DoesNotDeleteKey_WhenCountRemainsPositive() {

    // when
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.increment(redisKey)).thenReturn(2L);
    when(valueOperations.decrement(redisKey)).thenReturn(1L);

    Message<byte[]> subMessage = createMessage(StompCommand.SUBSCRIBE, validDestination, sessionId, true);
    listener.handleSubscribe(new SessionSubscribeEvent(this, subMessage, mock(Principal.class)));

    Message<byte[]> disconnectMessage = createMessage(StompCommand.DISCONNECT, null, sessionId, false);
    listener.handleLeave(new SessionDisconnectEvent(this, disconnectMessage, sessionId, null));

    // then
    verify(valueOperations).decrement(redisKey);
    verify(redisTemplate, never()).delete(redisKey);
 }
}