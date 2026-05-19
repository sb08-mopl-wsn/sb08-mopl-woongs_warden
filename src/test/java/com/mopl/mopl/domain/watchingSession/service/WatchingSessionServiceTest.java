package com.mopl.mopl.domain.watchingSession.service;

import com.mopl.mopl.domain.content.dto.response.ContentSummary;
import com.mopl.mopl.domain.content.entity.Content;
import com.mopl.mopl.domain.content.entity.ContentType;
import com.mopl.mopl.domain.content.exception.ContentNotFoundException;
import com.mopl.mopl.domain.content.repository.ContentRepository;
import com.mopl.mopl.domain.user.dto.UserSummary;
import com.mopl.mopl.domain.user.entity.User;
import com.mopl.mopl.domain.user.exception.UserNotFoundException;
import com.mopl.mopl.domain.user.repository.UserRepository;
import com.mopl.mopl.domain.watchingSession.dto.request.ContentChatSendRequest;
import com.mopl.mopl.domain.watchingSession.dto.request.WatchingSessionPageRequest;
import com.mopl.mopl.domain.watchingSession.dto.response.ContentChatDto;
import com.mopl.mopl.domain.watchingSession.dto.response.CursorResponseWatchingSessionDto;
import com.mopl.mopl.domain.watchingSession.dto.response.WatchingSessionDto;
import com.mopl.mopl.domain.watchingSession.dto.response.WatchingSessionSearchCondition;
import com.mopl.mopl.domain.watchingSession.entity.ChangeType;
import com.mopl.mopl.domain.watchingSession.entity.SortDirection;
import com.mopl.mopl.domain.watchingSession.entity.WatchingSession;
import com.mopl.mopl.domain.watchingSession.exception.WatchingSessionNotFoundException;
import com.mopl.mopl.domain.watchingSession.mapper.WatchingSessionMapper;
import com.mopl.mopl.domain.watchingSession.repository.WatchingSessionRepository;
import com.mopl.mopl.global.component.BadWordFilter;
import com.mopl.mopl.global.event.LiveChatEvent;
import com.mopl.mopl.global.event.WatchingSessionEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WatchingSessionService 테스트")
public class WatchingSessionServiceTest {

    @InjectMocks
    private WatchingSessionServiceImpl watchingSessionService;

    @Mock
    private WatchingSessionRepository watchingSessionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ContentRepository contentRepository;
    @Mock
    private WatchingSessionMapper sessionMapper;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private BadWordFilter badWordFilter;

    // common
    private UUID contentId;
    private UUID userId;
    private UUID sessionId;

    private User user;
    private Content content;
    private WatchingSession session;
    private WatchingSessionDto sessionDto;

    @BeforeEach
    void setUp() {
        contentId = UUID.randomUUID();
        userId = UUID.randomUUID();
        sessionId = UUID.randomUUID();

        user = User.builder()
                .name("테스트 유저")
                .email("test@test.com")
                .password("test1234!")
                .build();
        ReflectionTestUtils.setField(user, "id", userId);

        content = Content.builder()
                .title("테스트 콘텐츠")
                .description("테스트 콘텐츠 설명")
                .contentType(ContentType.movie)
                .thumbnailKey("thumbnail/image.jpg")
                .tags(List.of("액션", "모험"))
                .build();
        ReflectionTestUtils.setField(content, "id", contentId);

        session = WatchingSession.builder()
                .content(content)
                .user(user)
                .build();
        ReflectionTestUtils.setField(session, "id", sessionId);
        ReflectionTestUtils.setField(session, "createdAt", Instant.now());

        UserSummary watcherSummery = new UserSummary(
                userId,
                "테스트 유저", "profile/image.jpg"
        );
        ContentSummary contentSummary = new ContentSummary(
                contentId,
                ContentType.movie,
                "테스트 콘텐츠",
                "테스트 콘텐츠 설명",
                "thumbnail/image.jpg",
                List.of("액션", "모험"),
                BigDecimal.valueOf(4.5),
                10
        );
        sessionDto = new WatchingSessionDto(
                sessionId,
                session.getCreatedAt(),
                watcherSummery,
                contentSummary
        );
    }

    private WatchingSession createSession(Instant createdAt) {
        WatchingSession session = WatchingSession.builder()
                .content(content)
                .user(user)
                .build();
        ReflectionTestUtils.setField(session, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(session, "createdAt", createdAt);
        return session;
    }

    @Nested
    @DisplayName("join()")
    class Join {

        @Test
        @DisplayName("User가 없으면 UserNotFoundException을 던진다.")
        void userNotFound_throwsException() {
            // given
            given(userRepository.findById(userId)).willReturn(Optional.empty());

            // when
            assertThatThrownBy(() -> watchingSessionService.join(contentId, userId))
                    .isInstanceOf(UserNotFoundException.class);

            // then
            verifyNoInteractions(watchingSessionRepository, eventPublisher);
        }

        @Test
        @DisplayName("Content가 없으면 ContentNotFoundException을 던진다.")
        void contentNotFound_throwsException() {
            // given
            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(contentRepository.findById(contentId)).willReturn(Optional.empty());

            // when
            assertThatThrownBy(() -> watchingSessionService.join(contentId, userId))
                    .isInstanceOf(ContentNotFoundException.class);

            // then
            verifyNoInteractions(watchingSessionRepository, eventPublisher);
        }

        @Test
        @DisplayName("기존 세션이 있으면 새로 생성하지 않고 기존 세션으로 이벤트를 발행한다.")
        void existingSession_reusesSessionAndPublishesEvent() {
            // given
            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(contentRepository.findById(contentId)).willReturn(Optional.of(content));
            given(watchingSessionRepository.findByContentIdAndUserId(contentId, userId))
                    .willReturn(Optional.of(session));
            given(sessionMapper.toDto(eq(sessionId), any(), eq(content), eq(user)))
                    .willReturn(sessionDto);

            // when
            watchingSessionService.join(contentId, userId);

            // then
            verify(watchingSessionRepository, never()).saveAndFlush(any());

            ArgumentCaptor<WatchingSessionEvent> captor =
                    ArgumentCaptor.forClass(WatchingSessionEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());

            WatchingSessionEvent published = captor.getValue();
            assertThat(published.change().type()).isEqualTo(ChangeType.JOIN);
            assertThat(published.contentId()).isEqualTo(contentId);
        }

        @Test
        @DisplayName("세션이 없으면 새로 생성(saveAndFlush)하고 JOIN 이벤트를 발행한다.")
        void noSession_createsNewSessionAndPublishesJoinEvent() {
            // given
            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(contentRepository.findById(contentId)).willReturn(Optional.of(content));
            given(watchingSessionRepository.findByContentIdAndUserId(contentId, userId))
                    .willReturn(Optional.empty());
            given(watchingSessionRepository.saveAndFlush(any())).willReturn(session);
            given(sessionMapper.toDto(eq(sessionId), any(), eq(content), eq(user)))
                    .willReturn(sessionDto);

            // when
            watchingSessionService.join(contentId, userId);

            // then
            verify(watchingSessionRepository, times(1)).saveAndFlush(any(WatchingSession.class));
            verify(eventPublisher, times(1)).publishEvent(any(WatchingSessionEvent.class));
        }

        @Test
        @DisplayName("이벤트에 현재 시청자 수가 포함된다.")
        void event_containsCurrentWatcherCount() {
            // given
            ReflectionTestUtils.setField(content, "watcherCount", 7);
            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(contentRepository.findById(contentId)).willReturn(Optional.of(content));
            given(watchingSessionRepository.findByContentIdAndUserId(contentId, userId))
                    .willReturn(Optional.of(session));
            given(sessionMapper.toDto(eq(sessionId), any(), any(), any()))
                    .willReturn(sessionDto);

            // when
            watchingSessionService.join(contentId, userId);

            // then
            ArgumentCaptor<WatchingSessionEvent> captor =
                    ArgumentCaptor.forClass(WatchingSessionEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());

            assertThat(captor.getValue().change().watcherCount()).isEqualTo(7L);
        }
    }

    @Test
    @DisplayName("기존 세션이 있으면 watcherCount를 증가시키지 않는다.")
    void existingSession_doesNotIncrementWatcherCount() {
        // given
        ReflectionTestUtils.setField(content, "watcherCount", 3);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(contentRepository.findById(contentId)).willReturn(Optional.of(content));
        given(watchingSessionRepository.findByContentIdAndUserId(contentId, userId))
                .willReturn(Optional.of(session));
        given(sessionMapper.toDto(any(), any(), any(), any())).willReturn(sessionDto);

        // when
        watchingSessionService.join(contentId, userId);

        //then
        assertThat(content.getWatcherCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("신규 세션이면 watcherCount를 1 증가시킨다.")
    void newSession_incrementsWatcherCount() {
        // given
        ReflectionTestUtils.setField(content, "watcherCount", 2);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(contentRepository.findById(contentId)).willReturn(Optional.of(content));
        given(watchingSessionRepository.findByContentIdAndUserId(contentId, userId))
                .willReturn(Optional.empty());
        given(watchingSessionRepository.saveAndFlush(any())).willReturn(session);
        given(sessionMapper.toDto(any(), any(), any(), any())).willReturn(sessionDto);

        // when
        watchingSessionService.join(contentId, userId);

        // then
        assertThat(content.getWatcherCount()).isEqualTo(3);
    }

    @Nested
    @DisplayName("leave()")
    class Leave {

        @Test
        @DisplayName("시청 세션이 없으면 아무것도 실행하지 않는다.")
        void sessionNotFound_doesNothing() {
            // given
            given(watchingSessionRepository.findByContentIdAndUserId(contentId, userId))
                    .willReturn(Optional.empty());

            // when & then
            watchingSessionService.leave(contentId, userId);

            verify(watchingSessionRepository, never()).delete(any());
            verify(watchingSessionRepository, never()).flush();
            verifyNoInteractions(eventPublisher);
        }

        @Test
        @DisplayName("세션 삭제 후 반드시 flush가 호출된다.(쓰기 지연 방지)")
        void leave_deleteThenFlushInOrder() {
            // given
            given(watchingSessionRepository.findByContentIdAndUserId(contentId, userId))
                    .willReturn(Optional.of(session));
            given(sessionMapper.toDto(eq(sessionId), any(), eq(content), eq(user)))
                    .willReturn(sessionDto);

            // when
            watchingSessionService.leave(contentId, userId);

            // then
            // delete -> flush 순서 보장하는가?
            InOrder inOrder = inOrder(watchingSessionRepository, eventPublisher);
            inOrder.verify(watchingSessionRepository).delete(session);
            inOrder.verify(watchingSessionRepository).flush();
            inOrder.verify(eventPublisher).publishEvent(any(WatchingSessionEvent.class));
        }

        @Test
        @DisplayName("삭제 전 세션 정보로 LEAVE 이벤트를 발행한다.")
        void leave_publishesEventWithDeletionSessionData() {
            // given
            given(watchingSessionRepository.findByContentIdAndUserId(contentId, userId))
                    .willReturn(Optional.of(session));
            given(sessionMapper.toDto(eq(sessionId), any(), eq(content), eq(user)))
                    .willReturn(sessionDto);

            // when
            watchingSessionService.leave(contentId, userId);

            // then
            ArgumentCaptor<WatchingSessionEvent> captor =
                    ArgumentCaptor.forClass(WatchingSessionEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());

            WatchingSessionEvent published = captor.getValue();
            assertThat(published.change().type()).isEqualTo(ChangeType.LEAVE);
            assertThat(published.change().watchingSession()).isEqualTo(sessionDto);
            assertThat(published.contentId()).isEqualTo(contentId);
        }

        @Test
        @DisplayName("LEAVE 이벤트 삭제 후 시청자 수가 반영된다.")
        void leave_watcherCountReflectsAfterDeletion() {
            // given
            ReflectionTestUtils.setField(content, "watcherCount", 1);
            given(watchingSessionRepository.findByContentIdAndUserId(contentId, userId))
                    .willReturn(Optional.of(session));
            given(sessionMapper.toDto(any(), any(), any(), any())).willReturn(sessionDto);

            // when
            watchingSessionService.leave(contentId, userId);

            // then
            ArgumentCaptor<WatchingSessionEvent> captor =
                    ArgumentCaptor.forClass(WatchingSessionEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());

            assertThat(captor.getValue().change().watcherCount()).isZero();
        }

        @Test
        @DisplayName("세션 삭제 시 watcherCount를 1 감소시킨다.")
        void leave_decrementsWatcherCount() {
            // given
            ReflectionTestUtils.setField(content, "watcherCount", 3);
            given(watchingSessionRepository.findByContentIdAndUserId(contentId, userId))
                    .willReturn(Optional.of(session));
            given(sessionMapper.toDto(any(), any(), any(), any())).willReturn(sessionDto);

            // when
            watchingSessionService.leave(contentId, userId);

            // then
            assertThat(content.getWatcherCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("watcherCount가 0일 때 leave() 호출 시 음수가 되지 않는다.")
        void leave_watcherCountDoesNotGoBelowZero() {
            // given
            ReflectionTestUtils.setField(content, "watcherCount", 0);
            given(watchingSessionRepository.findByContentIdAndUserId(contentId, userId))
                    .willReturn(Optional.of(session));
            given(sessionMapper.toDto(any(), any(), any(), any())).willReturn(sessionDto);

            // when
            watchingSessionService.leave(contentId, userId);

            // then
            assertThat(content.getWatcherCount()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("receiveMessage()")
    class ReceiveMessage {

        private ContentChatSendRequest chatRequest;

        @BeforeEach
        void setUp() {
            chatRequest = new ContentChatSendRequest("테스트 메시지");
        }

        @Test
        @DisplayName("User가 없으면 UserNotFoundException을 던진다.")
        void userNotFound_throwsException() {
            // given
            given(userRepository.findById(userId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                    watchingSessionService.receiveMessage(contentId, userId, chatRequest))
                    .isInstanceOf(UserNotFoundException.class);
            verifyNoInteractions(eventPublisher);
        }

        @Test
        @DisplayName("Content가 없으면 ContentNotFoundException을 던진다.")
        void contentNotFound_throwsException() {
            // given
            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(contentRepository.existsById(contentId)).willReturn(false);

            // when & then
            assertThatThrownBy(() ->
                    watchingSessionService.receiveMessage(contentId, userId, chatRequest))
                    .isInstanceOf(ContentNotFoundException.class);
            verifyNoInteractions(eventPublisher);
        }

        @Test
        @DisplayName("시청 세션 없이 채팅을 보내면 WatchingSessionNotFoundException을 던진다.")
        void noWatchingSession_throwsException() {
            // given
            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(contentRepository.existsById(contentId)).willReturn(true);
            given(watchingSessionRepository.findByContentIdAndUserId(contentId, userId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                    watchingSessionService.receiveMessage(contentId, userId, chatRequest))
                    .isInstanceOf(WatchingSessionNotFoundException.class);
            verifyNoInteractions(eventPublisher);
        }

        @Test
        @DisplayName("정상 조건이면 LiveChatEvent가 발행된다.")
        void validRequest_publishesLiveChatEvent() {
            String rawMessage = "바보 같은 메시지";
            String maskedMessage = "** 같은 메시지";

            ContentChatSendRequest request = new ContentChatSendRequest(rawMessage);

            // given
            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(contentRepository.existsById(contentId)).willReturn(true);
            given(watchingSessionRepository.findByContentIdAndUserId(contentId, userId))
                    .willReturn(Optional.of(session));
            given(badWordFilter.maskBadWord(rawMessage)).willReturn(maskedMessage);
            ContentChatDto chatDto = new ContentChatDto(new UserSummary(userId, "유저", "image.jpg"), maskedMessage);
            given(sessionMapper.toChatDto(any(UserSummary.class), eq(maskedMessage)))
                    .willReturn(chatDto);

            // when
            watchingSessionService.receiveMessage(contentId, userId, request);

            // then
            ArgumentCaptor<LiveChatEvent> captor =
                    ArgumentCaptor.forClass(LiveChatEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());

            LiveChatEvent published = captor.getValue();
            assertThat(published.contentId()).isEqualTo(contentId);
            assertThat(published.chatDto().content()).isEqualTo(maskedMessage);
        }
    }

    @Nested
    @DisplayName("findByContentInWatchingSession()")
    class FindByContentInWatchingSession {

        private WatchingSessionPageRequest buildRequest(int limit) {
            return new WatchingSessionPageRequest(
                    null,
                    null,
                    null,
                    limit,
                    SortDirection.DESCENDING,
                    "createdAt"
            );
        }

        private WatchingSession buildMockSession(Instant createdAt) {
            return createSession(createdAt);
        }

        @Test
        @DisplayName("결과가 limit 이하이면 hasNext=false이고 전체 목록을 반환한다.")
        void resultBelowLimit_hasNextFalse() {
            WatchingSessionPageRequest request = buildRequest(10);

            List<WatchingSession> sessions = List.of(
                    buildMockSession(Instant.now()),
                    buildMockSession(Instant.now())
            );

            // given
            given(watchingSessionRepository.findAllByCursor(
                    any(WatchingSessionSearchCondition.class), any(Pageable.class)
            )).willReturn(sessions);
            given(watchingSessionRepository.countByContentId(contentId)).willReturn(2L);
            given(sessionMapper.toDto(any(WatchingSession.class))).willReturn(sessionDto);

            // when
            CursorResponseWatchingSessionDto result =
                    watchingSessionService.findByContentInWatchingSession(contentId, request);

            // then
            assertThat(result.hasNext()).isFalse();
            assertThat(result.data()).hasSize(2);
        }

        @Test
        @DisplayName("결과가 limit+1이면 hasNext=true이고 마지막 요소는 제거된다.")
        void resultExceedLimit_hasNextTrueAndLastRemoved() {
            WatchingSessionPageRequest request = buildRequest(2);
            Instant t1 = Instant.now().minusSeconds(10);
            Instant t2 = Instant.now().minusSeconds(5);
            Instant t3 = Instant.now();

            WatchingSession s1 = buildMockSession(t1);
            WatchingSession s2 = buildMockSession(t2);
            WatchingSession s3 = buildMockSession(t3);

            // given
            given(watchingSessionRepository.findAllByCursor(any(), any()))
                    .willReturn(new ArrayList<>(List.of(s1, s2, s3)));

            given(watchingSessionRepository.countByContentId(contentId)).willReturn(3L);
            given(sessionMapper.toDto(any(WatchingSession.class))).willReturn(sessionDto);

            // when
            CursorResponseWatchingSessionDto result =
                    watchingSessionService.findByContentInWatchingSession(contentId, request);

            // then
            assertThat(result.hasNext()).isTrue();
            assertThat(result.data()).hasSize(2);
        }

        @Test
        @DisplayName("nextCursor와 nextIdAfter는 마지막 세션 기준으로 설정된다.")
        void nextCursorAndIdAfter_setFromLastSession() {
            WatchingSessionPageRequest request = buildRequest(10);
            Instant lastCreatedAt = Instant.parse("2025-01-01T12:00:00Z");

            WatchingSession last = buildMockSession(lastCreatedAt);
            UUID lastId = last.getId();

            // given
            given(watchingSessionRepository.findAllByCursor(any(), any()))
                    .willReturn(new ArrayList<>(List.of(buildMockSession(Instant.now()), last)));
            given(watchingSessionRepository.countByContentId(contentId)).willReturn(2L);
            given(sessionMapper.toDto(any(WatchingSession.class)))
                    .willReturn(sessionDto);

            // when
            CursorResponseWatchingSessionDto result =
                    watchingSessionService.findByContentInWatchingSession(contentId, request);

            // then
            assertThat(result.nextCursor()).isEqualTo(lastCreatedAt.toString());
            assertThat(result.nextIdAfter()).isEqualTo(lastId);
        }

        @Test
        @DisplayName("결과가 비어 있으면 nextCursor와 nextIdAfter는 null이다.")
        void emptyResult_nextCursorAndIdAfterIsNull() {
            WatchingSessionPageRequest request = buildRequest(10);

            // given
            given(watchingSessionRepository.findAllByCursor(any(), any()))
                    .willReturn(Collections.emptyList());
            given(watchingSessionRepository.countByContentId(contentId)).willReturn(0L);

            // when
            CursorResponseWatchingSessionDto result =
                    watchingSessionService.findByContentInWatchingSession(contentId, request);

            // then
            assertThat(result.nextCursor()).isNull();
            assertThat(result.nextIdAfter()).isNull();
            assertThat(result.hasNext()).isFalse();
            assertThat(result.data()).isEmpty();
        }

        @Test
        @DisplayName("pageable은 limit+1로 요청된다.")
        void pageable_requestedWithLimitPlusOne() {
            int limit = 5;
            WatchingSessionPageRequest request = buildRequest(limit);

            // given
            given(watchingSessionRepository.findAllByCursor(any(), any()))
                    .willReturn(Collections.emptyList());
            given(watchingSessionRepository.countByContentId(contentId))
                    .willReturn(0L);

            // when
            watchingSessionService.findByContentInWatchingSession(contentId, request);

            // then
            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(watchingSessionRepository).findAllByCursor(any(), pageableCaptor.capture());
            assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(limit + 1);
        }

        @Test
        @DisplayName("totalCount는 항상 countByContentId의 반환값을 사용한다.")
        void totalCount_usesCountByContentId() {
            WatchingSessionPageRequest request = buildRequest(10);

            // given
            given(watchingSessionRepository.findAllByCursor(any(), any()))
                    .willReturn(Collections.emptyList());
            given(watchingSessionRepository.countByContentId(contentId)).willReturn(10L);

            // when
            CursorResponseWatchingSessionDto result =
                    watchingSessionService.findByContentInWatchingSession(contentId, request);

            // then
            assertThat(result.totalCount()).isEqualTo(10L);
        }
    }

    @Nested
    @DisplayName("findCurrentWatchingSessionByUserId()")
    class FindCurrentWatchingSessionByUserId {

        @Test
        @DisplayName("userId == currentUserId이면 DB조회 없이 Optional.empty()를 반환한다.")
        void sameUser_returnsEmptyWithoutDB() {

            // when
            Optional<WatchingSessionDto> result =
                    watchingSessionService.findCurrentWatchingSessionByUserId(userId, userId);

            // then
            assertThat(result).isEmpty();
            verifyNoInteractions(userRepository, watchingSessionRepository);
        }

        @Test
        @DisplayName("User가 없으면 UserNotFoundException을 던진다.")
        void userNotFound_throwsException() {
            UUID currentUserId = UUID.randomUUID();

            // given
            given(userRepository.existsById(userId)).willReturn(false);

            // when & then
            assertThatThrownBy(() ->
                    watchingSessionService.findCurrentWatchingSessionByUserId(userId, currentUserId))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        @DisplayName("시청 중인 세션이 없으면 Optional.empty()를 반환한다.")
        void noSession_returnsEmpty() {
            UUID currentUserId = UUID.randomUUID();

            // given
            given(userRepository.existsById(userId)).willReturn(true);
            given(watchingSessionRepository.findFirstByUserIdOrderByCreatedAtDesc(userId))
                    .willReturn(Optional.empty());

            // when
            Optional<WatchingSessionDto> result =
                    watchingSessionService.findCurrentWatchingSessionByUserId(userId, currentUserId);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("시청 중인 세션이 있으면 매핑된 DTO를 반환한다.")
        void sessionExists_returnsDto() {
            UUID currentUserId = UUID.randomUUID();

            // given
            given(userRepository.existsById(userId)).willReturn(true);
            given(watchingSessionRepository.findFirstByUserIdOrderByCreatedAtDesc(userId))
                    .willReturn(Optional.of(session));
            given(sessionMapper.toDto(session)).willReturn(sessionDto);

            // when
            Optional<WatchingSessionDto> result =
                    watchingSessionService.findCurrentWatchingSessionByUserId(userId, currentUserId);

            // then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(sessionDto);
        }

        @Test
        @DisplayName("자기 자신 조회 시 userRepository.existsById는 호출되지 않는다.")
        void selfQuery_skipsUserExistenceCheck() {
            // when
            watchingSessionService.findCurrentWatchingSessionByUserId(userId, userId);

            // then
            verify(userRepository, never()).existsById(any());
        }
    }
}
