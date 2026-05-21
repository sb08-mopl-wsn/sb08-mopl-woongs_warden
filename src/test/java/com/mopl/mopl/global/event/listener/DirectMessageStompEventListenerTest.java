package com.mopl.mopl.global.event.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import com.mopl.mopl.domain.user.dto.UserDto;
import com.mopl.mopl.domain.user.entity.Role;
import com.mopl.mopl.global.auth.details.MoplUserDetails;
import java.security.Principal;
import java.time.Instant;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
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
}