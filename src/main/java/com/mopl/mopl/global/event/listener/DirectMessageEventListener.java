package com.mopl.mopl.global.event.listener;

import com.mopl.mopl.global.config.AsyncConfig;
import com.mopl.mopl.global.event.DirectMessageCreatedEvent;
import com.mopl.mopl.global.sse.service.SseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class DirectMessageEventListener {

  private final SseService sseService;

  @Async(AsyncConfig.NOTIFICATION_EXECUTOR)
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onDirectMessageCreated(DirectMessageCreatedEvent event) {

    log.debug("DM 이벤트 수신 - receiver: {}", event.receiverId());

    // SSE 푸시 알림 발송
    sseService.sendNotification(
        event.receiverId(),
        "새로운 메시지가 도착했습니다: " + event.content()
    );
  }
}
