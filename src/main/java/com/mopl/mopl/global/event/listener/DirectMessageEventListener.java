package com.mopl.mopl.global.event.listener;

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

  private static final String DIRECT_MESSAGE_PREFIX = "/sub/conversations/";
  private static final String DIRECT_MESSAGE_SUFFIX = "/direct-messages";

  @Async(AsyncConfig.NOTIFICATION_EXECUTOR)
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onDirectMessageCreated(DirectMessageCreatedEvent event) {

    log.debug("DM 이벤트 수신 - messageId: {}", event.messageId());

    // SSE 푸시 알림 발송
    try {
      sseService.sendNotification(
          event.receiverId(),
          "새로운 메시지가 도착했습니다: " + event.content()
      );
    } catch (Exception e) {
      log.warn("DM SSE 전송 실패 - messageId: {}, receiverId: {}", event.messageId(), event.receiverId(), e);
    }
  }

  @Async(AsyncConfig.DIRECT_MESSAGE_EXECUTOR)
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onDirectMessageSent(DirectMessageSentEvent event) {
    log.debug("DM Websocket 전송 - conversationId: {}", event.conversationId());

    String destination = createDirectMessageDestination(event.conversationId());

    messagingTemplate.convertAndSend(destination, event.messageDto());
  }

  private String createDirectMessageDestination(UUID conversationId) {
    return DIRECT_MESSAGE_PREFIX + conversationId + DIRECT_MESSAGE_SUFFIX;
  }
}
