package com.mopl.mopl.global.event.listener;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.mopl.mopl.domain.dm.dto.DirectMessageDto;
import com.mopl.mopl.domain.dm.service.RoomPresenceManager;
import com.mopl.mopl.domain.user.dto.UserSummary;
import com.mopl.mopl.global.event.DirectMessageCreatedEvent;
import com.mopl.mopl.global.event.DirectMessageSentEvent;
import com.mopl.mopl.global.sse.service.SseService;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
class DirectMessageEventListenerTest {

  @InjectMocks
  private DirectMessageEventListener directMessageEventListener;

  @Mock
  private SseService sseService;
  @Mock
  private SimpMessagingTemplate messagingTemplate;
  @Mock
  private RoomPresenceManager roomPresenceManager;

  private UUID conversationId;
  private UUID receiverId;
  private DirectMessageDto messageDto;

  @BeforeEach
  void setUp() {
    conversationId = UUID.randomUUID();
    receiverId = UUID.randomUUID();

    UserSummary senderSummary = new UserSummary(UUID.randomUUID(), "보낸이", null);
    messageDto = new DirectMessageDto(
        UUID.randomUUID(), conversationId, "안녕하세요", senderSummary, null, Instant.now()
    );
  }

  @Test
  @DisplayName("DM 생성 이벤트 - 수신자가 채팅창 밖에 있으면 SSE 알림을 발송한다.")
  void onDirectMessageCreated_ReceiverNotInRoom_SendsSse() {

    // given
    DirectMessageCreatedEvent event = DirectMessageCreatedEvent.of(conversationId, receiverId, messageDto);

    given(roomPresenceManager.isUserInRoom(receiverId, conversationId)).willReturn(false);

    // when
    directMessageEventListener.onDirectMessageCreated(event);

    // then
    verify(sseService).sendCustomNotification(eq(receiverId), eq("direct-messages"), eq(messageDto));
  }

  @Test
  @DisplayName("DM 생성 이벤트 - 수신자가 채팅방 안에 있으면 SSE 알림을 발송하지 않는다.")
  void onDirectMessageCreated_ReceiverInRoom_SkipsSse() {

    // given
    DirectMessageCreatedEvent event = DirectMessageCreatedEvent.of(conversationId, receiverId, messageDto);

    given(roomPresenceManager.isUserInRoom(receiverId, conversationId)).willReturn(true);

    // when
    directMessageEventListener.onDirectMessageCreated(event);

    // then
    verify(sseService, never()).sendCustomNotification(any(), any(), any());
  }

  @Test
  @DisplayName("DM 발송 이벤트 - 올바른 STOMP 경로로 채팅 메시지를 브로드캐스팅한다.")
  void onDirectMessageSent_BroadcastsToStompDestination() {

    // given
    DirectMessageSentEvent event = DirectMessageSentEvent.of(conversationId, messageDto);
    String expectedDestination = "/sub/conversations/" + conversationId + "/direct-messages";

    // when
    directMessageEventListener.onDirectMessageSent(event);

    // then
    verify(messagingTemplate).convertAndSend(eq(expectedDestination), eq(messageDto));
  }

  @Test
  @DisplayName("DM 생성 이벤트 - SSE 발송 중 예외가 발송해도 스레드가 중단되지 않고 예외를 삼킨다.")
  void onDirectMessageCreated_SseException_HandledGracefully() {

    // given
    DirectMessageCreatedEvent event = DirectMessageCreatedEvent.of(conversationId, receiverId, messageDto);
    given(roomPresenceManager.isUserInRoom(receiverId, conversationId)).willReturn(false);

    doThrow(new RuntimeException("SSE 전송 네트워크 에러 발생"))
        .when(sseService).sendCustomNotification(any(), any(), any());

    // when & then
    assertDoesNotThrow(() -> directMessageEventListener.onDirectMessageCreated(event));
    verify(sseService).sendCustomNotification(eq(receiverId), eq("direct-messages"), eq(messageDto));
  }

  @Test
  @DisplayName("DM 발송 이벤트 - STOMP 브로드캐스팅 중 예외가 발생해도 스레드가 중단되지 않고 예외를 삼킨다.")
  void onDirectMessageSent_StompException_HandledGracefully() {

    // given
    DirectMessageSentEvent event = DirectMessageSentEvent.of(conversationId, messageDto);
    String expectedDestination = "/sub/conversations/" + conversationId + "/direct-messages";

    doThrow(new RuntimeException("STOMP 브로커 다운"))
        .when(messagingTemplate).convertAndSend(any(String.class), any(Object.class));

    // when & then
    assertDoesNotThrow(() -> directMessageEventListener.onDirectMessageSent(event));
    verify(messagingTemplate).convertAndSend(eq(expectedDestination), eq(messageDto));
  }
}