package com.mopl.mopl.global.event.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.mopl.mopl.domain.dm.service.RoomPresenceManager;
import com.mopl.mopl.domain.user.dto.UserDto;
import com.mopl.mopl.domain.user.entity.Role;
import com.mopl.mopl.global.auth.details.MoplUserDetails;
import com.mopl.mopl.global.event.DirectMessageReadEvent;
import java.security.Principal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

@Slf4j
@ExtendWith(MockitoExtension.class)
class DirectMessageStompEventListenerTest {

  @InjectMocks
  private DirectMessageStompEventListener listener;

  @Mock
  private SimpMessagingTemplate simpMessagingTemplate;

  private UUID userId;
  private UUID conversationId;
  private String sessionId;
  private String validDestination;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    conversationId = UUID.randomUUID();
    sessionId = "session-123";
    validDestination = "/sub/conversations/" + conversationId + "/direct-messages";
  }

  // 가짜 인증 토큰 생성 헬퍼 메서드
  private UsernamePasswordAuthenticationToken createAuthToken(UUID userId) {
    UserDto userDto = new UserDto(userId, Instant.now(), "test@test.com", "테스트", null, Role.USER, false);
    MoplUserDetails userDetails = new MoplUserDetails(userDto, "password");
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
  @DisplayName("정상 경로 구독 시 사용자가 채팅방에 입장한 것으로 처리된다.")
  void handleSubscribe_Success() {
    Message<byte[]> message = createMessage(StompCommand.SUBSCRIBE, validDestination, sessionId, true);
    SessionSubscribeEvent event = new SessionSubscribeEvent(this, message, mock(Principal.class));

    listener.handleSubscribe(event);

    assertThat(listener.isUserInRoom(userId, conversationId)).isTrue();
  }

  @Test
  @DisplayName("지정되지 않은 경로를 구독하면 무시된다.")
  void handleSubscribe_InvalidDestination_Ignored() {
    Message<byte[]> message = createMessage(StompCommand.SUBSCRIBE, "/sub/wrong/path", sessionId, true);
    SessionSubscribeEvent event = new SessionSubscribeEvent(this, message, mock(Principal.class));

    listener.handleSubscribe(event);

    assertThat(listener.isUserInRoom(userId, conversationId)).isFalse();
  }

  @Test
  @DisplayName("같은 방을 다중 탭(멀티 세션)으로 켠 상태에서 탭 하나를 닫아도 여전히 방에 있는 것으로 처리된다.")
  void multiTabSubscribe_And_Leave() {

    // given
    String sessionId2 = "session-456";
    Message<byte[]> message = createMessage(StompCommand.SUBSCRIBE, validDestination, sessionId, true);
    Message<byte[]> message2 = createMessage(StompCommand.SUBSCRIBE, validDestination, sessionId2, true);

    listener.handleSubscribe(new SessionSubscribeEvent(this, message, mock(Principal.class)));
    listener.handleSubscribe(new SessionSubscribeEvent(this, message2, mock(Principal.class)));

    assertThat(listener.isUserInRoom(userId, conversationId)).isTrue();

    // when
    Message<byte[]> unMessage = createMessage(StompCommand.UNSUBSCRIBE, null, sessionId, false);
    listener.handleUnSubscribe(new SessionUnsubscribeEvent(this, unMessage, mock(Principal.class)));

    // then -> 아직 두번째 탭이 살아있으므로 여전히 방에 있는 상태
    assertThat(listener.isUserInRoom(userId, conversationId)).isTrue();

    // when 나머지 두번째 탭도 종료
    Message<byte[]> unMessage2 = createMessage(StompCommand.UNSUBSCRIBE, null, sessionId2, false);
    listener.handleUnSubscribe(new SessionUnsubscribeEvent(this, unMessage2, mock(Principal.class)));

    // then 모든 탭 꺼짐
    assertThat(listener.isUserInRoom(userId, conversationId)).isFalse();
  }

  @Test
  @DisplayName("구독 시 인증 정보(Principal)가 없으면 예외 발생")
  void handleSubscribe_NoAuth_HandledGracefully() {

    // given
    Message<byte[]> message = createMessage(StompCommand.SUBSCRIBE, validDestination, sessionId, false);
    SessionSubscribeEvent event = new SessionSubscribeEvent(this, message, null);

    // when & then
    assertDoesNotThrow(() -> listener.handleSubscribe(event));
    assertThat(listener.isUserInRoom(userId, conversationId)).isFalse();
  }

  @Test
  @DisplayName("구독 처리 중 알 수 없는 에러가 발생해도 서버가 죽지 않고 예외를 삼킨다.")
  void handleSubscribe_InternalError_HandleGracefully() {

    UsernamePasswordAuthenticationToken badAuth = mock(UsernamePasswordAuthenticationToken.class);
    doThrow(new RuntimeException("의도적인 내부 상태 조회 에러 발생")).when(badAuth).getPrincipal();

    // given
    Message<byte[]> message = createMessage(StompCommand.SUBSCRIBE, validDestination, sessionId, false);
    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
    accessor.setUser(badAuth);

    Message<byte[]> badMessage = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    SessionSubscribeEvent event = new SessionSubscribeEvent(this, badMessage, badAuth);

    // when & then
    assertDoesNotThrow(() -> listener.handleSubscribe(event));
    assertThat(listener.isUserInRoom(userId, conversationId)).isFalse();
  }

  @Test
  @DisplayName("구독 경로의 UUID 형식이 잘못되면 예외 발생")
  void handleSubscribe_MalformedUuid_HandleGracefully() {

    // given
    String badDestination = "/sub/conversations/invalid-uuid-format/direct-messages";
    Message<byte[]> message = createMessage(StompCommand.SUBSCRIBE, badDestination, sessionId, true);

    Principal principal = mock(Principal.class);
    SessionSubscribeEvent event = new SessionSubscribeEvent(this, message, principal);

    // when & then
    assertDoesNotThrow(() -> listener.handleSubscribe(event));
    assertThat(listener.isUserInRoom(userId, conversationId)).isFalse();
  }

  @Test
  @DisplayName("구독 취소(Unsubscribe) 이벤트 수신 시 예외 없이 세션이 제거된다.")
  void handleUnSubscribe_ExplicitCall_Success() {

    // given
    Message<byte[]> message = createMessage(StompCommand.UNSUBSCRIBE, null, sessionId, false);
    Principal principal = mock(Principal.class);
    SessionUnsubscribeEvent event = new SessionUnsubscribeEvent(this, message, principal);

    // when & then
    assertDoesNotThrow(() -> listener.handleUnSubscribe(event));
  }

  @Test
  @DisplayName("DM 읽음 처리 이벤트 - STOMP로 읽음 Watermark 신호를 브로드캐스팅한다.")
  void onDirectMessageRead_BroadcastsWatermark() {

    // given
    Instant now = Instant.now();
    DirectMessageReadEvent event = DirectMessageReadEvent.of(conversationId, userId, now);

    String expectedDestination = "/sub/conversations/" + conversationId + "/direct-messages";

    // when
    listener.onDirectMessageRead(event);

    // then
    ArgumentCaptor<Map> payloadCaptor = ArgumentCaptor.forClass(Map.class);
    verify(simpMessagingTemplate).convertAndSend(eq(expectedDestination), payloadCaptor.capture());

    Map<String, Object> payload = payloadCaptor.getValue();
    assertThat(payload.get("type")).isEqualTo("READ_WATERMARK");
    assertThat(payload.get("readerId")).isEqualTo(userId);
    assertThat(payload.get("readAt")).isEqualTo(now);
  }
}