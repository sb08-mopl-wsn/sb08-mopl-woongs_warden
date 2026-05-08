package com.mopl.mopl.global.event.listener;

import com.mopl.mopl.global.event.WatchingSessionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@RequiredArgsConstructor
@Component
public class WatchingSessionEventListener {

    private final SimpMessagingTemplate messagingTemplate;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleWatchingSessionEvent(WatchingSessionEvent event) {
        messagingTemplate.convertAndSend(
                "/sub/contents/" + event.contentId() + "/watch", event.change()
        );
    }
}
