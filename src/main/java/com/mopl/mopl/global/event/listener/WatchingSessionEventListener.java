package com.mopl.mopl.global.event.listener;

import com.mopl.mopl.global.event.WatchingSessionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Component
public class WatchingSessionEventListener {

    private final SimpMessagingTemplate messagingTemplate;

    // Constant
    private static final String DESTINATION_PREFIX = "/sub/contents/";
    private static final String DESTINATION_SUFFIX = "/watch";

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleWatchingSessionEvent(WatchingSessionEvent event) {
        String destination = createDestination(event.contentId());

        log.info("[WatchingSessionEvent] 메시지 발행 - Destination: {}, Type: {}",
                destination, event.change().type());

        messagingTemplate.convertAndSend(destination, event.change());
    }

    private String createDestination(UUID contentId) {
        return DESTINATION_PREFIX + contentId + DESTINATION_SUFFIX;
    }
}
