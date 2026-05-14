package com.mopl.mopl.global.event.listener;

import com.mopl.mopl.domain.dm.service.RoomPresenceManager;
import com.mopl.mopl.global.config.AsyncConfig;
import com.mopl.mopl.global.event.DirectMessageCreatedEvent;
import com.mopl.mopl.global.event.DirectMessageSentEvent;
import com.mopl.mopl.global.sse.service.SseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DirectMessageEventListener {

  private final SseService sseService;
  private final SimpMessagingTemplate messagingTemplate;

  private final RoomPresenceManager roomPresenceManager;

  private static final String DIRECT_MESSAGE_PREFIX = "/sub/conversations/";
  private static final String DIRECT_MESSAGE_SUFFIX = "/direct-messages";

  @Async(AsyncConfig.NOTIFICATION_EXECUTOR)
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onDirectMessageCreated(DirectMessageCreatedEvent event) {

    log.debug("DM 이벤트 수신 - messageId: {}", event.messageDto().id());

    // SSE 푸시 알림 발송
    try {
      // 수신자가 현재 채팅방을 열어두고 웹소켓으로 보고 있는지 여부
      boolean isInRoom = roomPresenceManager.isUserInRoom(event.receiverId(), event.conversationId());

      // 채팅창 밖에 있을 때만 팝업(SSE) 푸시 전송
      if (!isInRoom) {
        sseService.sendCustomNotification(
            event.receiverId(),
            "direct-messages",
            event.messageDto()
        );

        log.debug("수신자가 방에 없어 SSE 알림을 발송했습니다. receiverId: {}", event.receiverId());
      } else {
        log.debug("수신자가 방에 접속 중이므로 SSE 알림을 생략합니다. receiverId: {}", event.receiverId());
      }
    } catch (Exception e) {
      log.warn("DM SSE 전송 실패 - receiverId: {}", event.receiverId(), e);
    }
  }

  @Async(AsyncConfig.DIRECT_MESSAGE_EXECUTOR)
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onDirectMessageSent(DirectMessageSentEvent event) {
    log.debug("DM Websocket 전송 - conversationId: {}", event.conversationId());

    String destination = createDirectMessageDestination(event.conversationId());

    try {
      messagingTemplate.convertAndSend(destination, event.messageDto());
    } catch (Exception e) {
      log.warn("DM WebSocket 전송 실패 - conversationId: {}, destination: {}", event.conversationId(), destination, e);
    }
  }

  private String createDirectMessageDestination(UUID conversationId) {
    return DIRECT_MESSAGE_PREFIX + conversationId + DIRECT_MESSAGE_SUFFIX;
  }
}
