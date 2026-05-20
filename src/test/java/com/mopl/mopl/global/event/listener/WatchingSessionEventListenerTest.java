package com.mopl.mopl.global.event.listener;

import com.mopl.mopl.domain.user.dto.UserSummary;
import com.mopl.mopl.domain.watchingSession.dto.response.ContentChatDto;
import com.mopl.mopl.domain.watchingSession.dto.response.WatchingSessionChange;
import com.mopl.mopl.domain.watchingSession.dto.response.WatchingSessionDto;
import com.mopl.mopl.domain.watchingSession.entity.ChangeType;
import com.mopl.mopl.global.event.LiveChatEvent;
import com.mopl.mopl.global.event.WatchingSessionEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WatchingSessionEventListener 테스트")
public class WatchingSessionEventListenerTest {

    @InjectMocks
    private WatchingSessionEventListener sessionEventListener;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    // common
    private UUID contentId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        contentId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("handleWatchingSessionEvent()")
    class HandleWatchingSessionEvent {

        @Test
        @DisplayName("정상 조건이면 트랜잭션 커밋 후 비동기로 시청 세션 변경 메시지를 발행된다.")
        void validRequest_publishesWatchingSessionMessage() {
            // given
            WatchingSessionDto sessionDto = new WatchingSessionDto(UUID.randomUUID(), Instant.now(), null, null);
            WatchingSessionChange change = new WatchingSessionChange(ChangeType.JOIN, sessionDto, 5L);
            WatchingSessionEvent event = new WatchingSessionEvent(change, contentId);

            String destination = "/sub/contents/" + contentId + "/watch";

            // when
            sessionEventListener.handleWatchingSessionEvent(event);

            // then
            verify(messagingTemplate, times(1))
                    .convertAndSend(eq(destination), eq(change));
        }

        @Test
        @DisplayName("정상 조건이면 트랜잭션 커밋 후 비동기로 라이브 채팅 메시지가 발행된다.")
        void validRequest_publishesLiveChatMessage() {
            // given
            UserSummary sender = new UserSummary(userId, "테스트 유저", "image.jpg");
            ContentChatDto chatDto = new ContentChatDto(sender, "** 같은 메시지");
            LiveChatEvent event = new LiveChatEvent(contentId, chatDto);

            String destination = "/sub/contents/" + contentId + "/chat";

            // when
            sessionEventListener.handleLiveChatEvent(event);

            // then
            ArgumentCaptor<ContentChatDto> captor = ArgumentCaptor.forClass(ContentChatDto.class);
            verify(messagingTemplate, times(1))
                    .convertAndSend(eq(destination), captor.capture());

            ContentChatDto sentDto = captor.getValue();
            assertThat(sentDto.content()).isEqualTo("** 같은 메시지");
            assertThat(sentDto.sender().userId()).isEqualTo(userId);
        }
    }
}