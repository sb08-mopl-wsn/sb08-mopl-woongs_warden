package com.mopl.mopl.global.event.listener;

import com.mopl.mopl.global.event.LiveChatEvent;
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
    private static final String SESSION_DESTINATION_SUFFIX = "/watch";
    private static final String LIVE_CHAT_DESTINATION_SUFFIX = "/chat";

    /**
     * 시청 세션의 상태 변경(입장, 퇴장) 이벤트를 처리한다.
     * 트랜잭션 커밋 이후에 실행되어 데이터 일관성을 보장하며,
     * 비동기로 실행되어 사용자의 응답 속도에 영향을 주지 않는다.
     *
     * @param event 시청 세션 변경 정보와 콘텐츠 ID를 포함한 이벤트 객체
     */

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleWatchingSessionEvent(WatchingSessionEvent event) {
        String destination = createSessionDestination(event.contentId());

        log.info("[WatchingSessionEvent] 메시지 발행 - Destination: {}, Type: {}",
                destination, event.change().type());

        messagingTemplate.convertAndSend(destination, event.change());
    }

    /**
     * 실시간 채팅 이벤트를 처리하여 해당 콘텐츠의 채팅방 구독자들에게 전달한다.
     * 별도의 스레드 풀을 사용하여 채팅 트래픽이 다른 비동기 작업에 영향을 주지 않도록 격리한다.
     *
     * @param event 채팅 내용과 발신자 정보가 담긴 이벤트 객체
     */
    @Async("watchingSessionExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleLiveChatEvent(LiveChatEvent event) {
        String destination = createLiveChatDestination(event.contentId());

        log.info("[WatchingSessionEvent] 라이브 채팅 메시지 발행 - Destination: {}, senderId: {}",
                destination, event.chatDto().sender().userId());

        messagingTemplate.convertAndSend(destination, event.chatDto());
    }

    // 시청 세션 메시지 전송 경로
    private String createSessionDestination(UUID contentId) {
        return DESTINATION_PREFIX + contentId + SESSION_DESTINATION_SUFFIX;
    }

    // 실시간 채팅 메시지 전송 경로
    private String createLiveChatDestination(UUID contentId) {
        return DESTINATION_PREFIX + contentId + LIVE_CHAT_DESTINATION_SUFFIX;
    }
}
