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
import com.mopl.mopl.infrastructure.s3.S3ImageStorage;
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
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

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
    @Mock
    private S3ImageStorage s3ImageStorage;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private SetOperations<String, String> setOperations;

    // common
    private UUID contentId;
    private UUID userId;
    private UUID sessionId;
    private String redisKey;

    private User user;
    private Content content;
    private WatchingSession session;
    private WatchingSessionDto sessionDto;

    @BeforeEach
    void setUp() {
        contentId = UUID.randomUUID();
        userId = UUID.randomUUID();
        sessionId = UUID.randomUUID();
        redisKey = "content:watcher:count:" + contentId;

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
            verifyNoInteractions(redisTemplate);

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
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(sessionMapper.toDto(eq(sessionId), any(), eq(content), eq(user)))
                    .willReturn(sessionDto);

            // when
            watchingSessionService.join(contentId, userId);

            // then
            verify(watchingSessionRepository, times(1)).saveAndFlush(any(WatchingSession.class));
            verify(valueOperations, times(1)).increment(redisKey);
            verify(redisTemplate, times(1)).expire(redisKey, 1, TimeUnit.DAYS);
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
    @DisplayName("기존 세션이 있으면 새로 생성하지 않고 Redis 카운트도 올리지 않는다.")
    void existingSession_reusesSessionAndPublishesEvent() {
        // given
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(contentRepository.findById(contentId)).willReturn(Optional.of(content));
        given(watchingSessionRepository.findByContentIdAndUserId(contentId, userId))
                .willReturn(Optional.of(session));
        given(sessionMapper.toDto(any(), any(), any(), any())).willReturn(sessionDto);

        // when
        watchingSessionService.join(contentId, userId);

        //then
        verify(watchingSessionRepository, never()).saveAndFlush(any());
        verifyNoInteractions(redisTemplate);

        ArgumentCaptor<WatchingSessionEvent> captor = ArgumentCaptor.forClass(WatchingSessionEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().change().type()).isEqualTo(ChangeType.JOIN);
    }

    @Test
    @DisplayName("세션이 없으면 신규 생성하고 Redis INCR 및 TTL을 갱신한다.")
    void newSession_incrementsWatcherCount() {
        // given
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(contentRepository.findById(contentId)).willReturn(Optional.of(content));
        given(watchingSessionRepository.findByContentIdAndUserId(contentId, userId))
                .willReturn(Optional.empty());
        given(watchingSessionRepository.saveAndFlush(any())).willReturn(session);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(sessionMapper.toDto(any(), any(), any(), any())).willReturn(sessionDto);

        // when
        watchingSessionService.join(contentId, userId);

        // then
        verify(watchingSessionRepository, times(1)).saveAndFlush(any(WatchingSession.class));
        verify(valueOperations, times(1)).increment(redisKey);
        verify(redisTemplate, times(1)).expire(redisKey, 1, TimeUnit.DAYS);
        verify(eventPublisher, times(1)).publishEvent(any(WatchingSessionEvent.class));
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
        @DisplayName("세션 삭제 후 Redis DECR이 순차적으로 정상 호출된다.")
        void leave_decrementsRedisCountAndFlushes() {
            // given
            given(watchingSessionRepository.findByContentIdAndUserId(contentId, userId))
                    .willReturn(Optional.of(session));
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.decrement(redisKey)).willReturn(5L);
            given(sessionMapper.toDto(eq(sessionId), any(), eq(content), eq(user)))
                    .willReturn(sessionDto);

            // when
            watchingSessionService.leave(contentId, userId);

            // then
            // delete -> flush 순서 보장하는가?
            InOrder inOrder = inOrder(watchingSessionRepository, valueOperations, eventPublisher);
            inOrder.verify(watchingSessionRepository).delete(session);
            inOrder.verify(watchingSessionRepository).flush();
            inOrder.verify(valueOperations).decrement(redisKey);
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
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.decrement(redisKey)).willReturn(10L);

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
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.decrement(redisKey)).willReturn(0L);

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
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.decrement(redisKey)).willReturn(2L);

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
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.decrement(redisKey)).willReturn(0L);

            // when
            watchingSessionService.leave(contentId, userId);

            // then
            assertThat(content.getWatcherCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("Redis DECR 결과가 음수 미만으로 떨어지면 0으로 보정한다.")
        void leave_preventsNegativeRedisCount() {
            given(watchingSessionRepository.findByContentIdAndUserId(contentId, userId))
                    .willReturn(Optional.of(session));
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.decrement(redisKey)).willReturn(-1L);
            given(sessionMapper.toDto(any(), any(), any(), any())).willReturn(sessionDto);

            watchingSessionService.leave(contentId, userId);

            verify(valueOperations).set(redisKey, "0");
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
            verify(badWordFilter, times(1)).maskBadWord(rawMessage);

            ArgumentCaptor<LiveChatEvent> captor =
                    ArgumentCaptor.forClass(LiveChatEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());

            LiveChatEvent published = captor.getValue();
            assertThat(published.contentId()).isEqualTo(contentId);
            assertThat(published.chatDto().content()).isEqualTo(maskedMessage);
        }

        @Test
        @DisplayName("정지된 유저가 채팅을 보내면 이벤트가 발행되지 않는다.")
        void bannedUser_doesNotPublishEvent() {
            // given
            ReflectionTestUtils.setField(user, "isBanned", true);
            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(contentRepository.existsById(contentId)).willReturn(true);

            // when
            watchingSessionService.receiveMessage(contentId, userId, chatRequest);

            // then
            verifyNoInteractions(eventPublisher);
            verifyNoInteractions(badWordFilter);
            verify(watchingSessionRepository, never())
                    .findByContentIdAndUserId(any(), any());
        }

        @Test
        @DisplayName("정지된 유저가 채팅을 보내면 시청 세션 검증을 수행하지 않는다.")
        void bannedUser_skipsWatchingSessionCheck() {
            // given
            ReflectionTestUtils.setField(user, "isBanned", true);
            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(contentRepository.existsById(contentId)).willReturn(true);

            // when
            watchingSessionService.receiveMessage(contentId, userId, chatRequest);

            // then
            verify(watchingSessionRepository, never())
                    .findByContentIdAndUserId(contentId, userId);
        }

        @Test
        @DisplayName("프로필 키가 비어있거나 (null) 없으면 S3 조회를 패스하고 null로 전송한다.")
        void validRequest_withNullProfileKey_skipS3AndPublishesEvent() {
            // given
            ReflectionTestUtils.setField(user, "profileImageKey", null);

            String rawMessage = "안녕하세요?";
            ContentChatSendRequest request = new ContentChatSendRequest(rawMessage);

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(contentRepository.existsById(contentId)).willReturn(true);
            given(watchingSessionRepository.findByContentIdAndUserId(contentId, userId))
                    .willReturn(Optional.of(session));
            given(badWordFilter.maskBadWord(rawMessage)).willReturn(rawMessage);

            ArgumentCaptor<UserSummary> userSummaryCaptor = ArgumentCaptor.forClass(UserSummary.class);
            ContentChatDto chatDto = new ContentChatDto(new UserSummary(userId, "테스트 유저", null), rawMessage);
            given(sessionMapper.toChatDto(userSummaryCaptor.capture(), eq(rawMessage))).willReturn(chatDto);

            watchingSessionService.receiveMessage(contentId, userId, request);

            verify(s3ImageStorage, never()).getPublicUrl(anyString());
            assertThat(userSummaryCaptor.getValue().profileImageUrl()).isNull();
        }

        @Test
        @DisplayName("프로필 키가 blank(공백)면 S3 조회를 패스하고 null로 전송한다.")
        void validRequest_withBlankProfileKey_skipS3AndPublishesEvent() {
            // given
            ReflectionTestUtils.setField(user, "profileImageKey", "   ");

            String rawMessage = "안녕하세요?";
            ContentChatSendRequest request = new ContentChatSendRequest(rawMessage);

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(contentRepository.existsById(contentId)).willReturn(true);
            given(watchingSessionRepository.findByContentIdAndUserId(contentId, userId))
                    .willReturn(Optional.of(session));
            given(badWordFilter.maskBadWord(rawMessage)).willReturn(rawMessage);

            ArgumentCaptor<UserSummary> userSummaryCaptor = ArgumentCaptor.forClass(UserSummary.class);
            ContentChatDto chatDto = new ContentChatDto(new UserSummary(userId, "테스트 유저", null), rawMessage);
            given(sessionMapper.toChatDto(userSummaryCaptor.capture(), eq(rawMessage))).willReturn(chatDto);

            // when
            watchingSessionService.receiveMessage(contentId, userId, request);

            // then
            verify(s3ImageStorage, never()).getPublicUrl(anyString());
            assertThat(userSummaryCaptor.getValue().profileImageUrl()).isNull();
        }

        @Test
        @DisplayName("정상 조건이면서 프로필 키가 존재할 때 S3 Public URL을 반영한 LiveChatEvent가 발행된다.")
        void validRequest_withProfileKey_publishesLiveChatEvent() {
            // given
            String rawMessage = "안녕하세요?";
            String maskedMessage = "안녕하세요?";
            String expectedCdnUrl = "https://cdn.mopl.com/profile/image.jpg";

            User testUser = User.builder()
                    .name("테스트 유저")
                    .email("test@test.com")
                    .password("test1234!")
                    .build();
            ReflectionTestUtils.setField(testUser, "id", userId);
            ReflectionTestUtils.setField(testUser, "profileImageKey", "profile/image.jpg");

            ContentChatSendRequest request = new ContentChatSendRequest(rawMessage);

            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));
            given(contentRepository.existsById(contentId)).willReturn(true);

            WatchingSession testSession = WatchingSession.builder()
                    .content(content)
                    .user(testUser)
                    .build();
            given(watchingSessionRepository.findByContentIdAndUserId(contentId, userId))
                    .willReturn(Optional.of(testSession));
            given(badWordFilter.maskBadWord(rawMessage)).willReturn(maskedMessage);

            given(s3ImageStorage.getPublicUrl(anyString())).willReturn(expectedCdnUrl);

            ArgumentCaptor<UserSummary> userSummaryCaptor = ArgumentCaptor.forClass(UserSummary.class);
            ContentChatDto chatDto = new ContentChatDto(new UserSummary(userId, "테스트 유저", expectedCdnUrl), maskedMessage);

            given(sessionMapper.toChatDto(userSummaryCaptor.capture(), eq(maskedMessage)))
                    .willReturn(chatDto);

            // when
            watchingSessionService.receiveMessage(contentId, userId, request);

            // then
            verify(s3ImageStorage, times(1)).getPublicUrl("profile/image.jpg");
            assertThat(userSummaryCaptor.getValue().profileImageUrl()).isEqualTo(expectedCdnUrl);

            ArgumentCaptor<LiveChatEvent> captor = ArgumentCaptor.forClass(LiveChatEvent.class);
            verify(eventPublisher, times(1)).publishEvent(captor.capture());
            assertThat(captor.getValue().chatDto().content()).isEqualTo(maskedMessage);
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
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get(redisKey)).willReturn(null);

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

            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get(redisKey)).willReturn(null);

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

            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get(redisKey)).willReturn(null);

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

            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get(redisKey)).willReturn(null);

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

            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get(redisKey)).willReturn(null);

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

            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get(redisKey)).willReturn(null);

            given(watchingSessionRepository.countByContentId(contentId)).willReturn(10L);

            // when
            CursorResponseWatchingSessionDto result =
                    watchingSessionService.findByContentInWatchingSession(contentId, request);

            // then
            assertThat(result.totalCount()).isEqualTo(10L);
        }

        @Test
        @DisplayName("Redis에 캐시된 값이 있다면 RDB count 쿼리를 생략하고 캐시 값을 파싱하여 반환한다.")
        void cacheHit_usesRedisCountWithRdbQuery() {
            // given
            WatchingSessionPageRequest request = buildRequest(10);
            given(watchingSessionRepository.findAllByCursor(any(), any()))
                    .willReturn(Collections.emptyList());

            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get(redisKey)).willReturn("45");

            // when
            CursorResponseWatchingSessionDto result =
                    watchingSessionService.findByContentInWatchingSession(contentId, request);

            // then
            assertThat(result.totalCount()).isEqualTo(45L);
            verify(watchingSessionRepository, never()).countByContentId(any());
            verify(valueOperations, never()).set(any(), any(), anyLong(), any());
        }
    }

    @Nested
    @DisplayName("findCurrentWatchingSessionByUserId()")
    class FindCurrentWatchingSessionByUserId {

        @Test
        @DisplayName("유저의 활성화된 웹소켓 세션 키가 레디스에 없으면 RDB 조회를 생략하고 empty를 반환한다.")
        void noUserSessionInRedis_returnsEmptyWithoutRdb() {
            UUID currentUserId = UUID.randomUUID();
            String userSessionKey = "ws:user:" + userId + ":sessions";

            // given
            given(redisTemplate.opsForSet()).willReturn(setOperations);
            given(setOperations.members(userSessionKey)).willReturn(Collections.emptySet());

            // when
            Optional<WatchingSessionDto> result =
                    watchingSessionService.findCurrentWatchingSessionByUserId(userId, currentUserId);

            // then
            assertThat(result).isEmpty();
            verifyNoInteractions(watchingSessionRepository, userRepository);
        }

        @Test
        @DisplayName("유저 세션은 존재하나 세션 내부 시청 룸 정보가 없으면 RDB 조회를 생략하고 empty를 반환한다.")
        void sessionExistsButNoActiveRoomInRedis_returnsEmptyWithoutRdb() {
            UUID currentUserId = UUID.randomUUID();
            String userSessionKey = "ws:user:" + userId + ":sessions";
            String mockSessionId = "mock-stomp-session-id";
            String sessionContentKey = "ws:session:" + mockSessionId + ":contents";

            //given
            given(redisTemplate.opsForSet()).willReturn(setOperations);
            given(setOperations.members(userSessionKey)).willReturn(new HashSet<>(List.of(mockSessionId)));
            given(setOperations.members(sessionContentKey)).willReturn(Collections.emptySet());

            // when
            Optional<WatchingSessionDto> result =
                    watchingSessionService.findCurrentWatchingSessionByUserId(userId, currentUserId);

            // then
            assertThat(result).isEmpty();
            verifyNoInteractions(watchingSessionRepository, userRepository);
        }

        @Test
        @DisplayName("Redis에서 시청 중인 룸 ID 역추적에 성공하면 RDB에서 DTO 스냅샷을 1회 조회하여 반환한다.")
        void redisSessionHit_fetchesDtoFromRdbAndReturns() {
            UUID currentUserId = UUID.randomUUID();
            String userSessionKey = "ws:user:" + userId + ":sessions";
            String mockSessionId = "mock-stomp-session-id";
            String sessionContentKey = "ws:session:" + mockSessionId + ":contents";

            // given
            given(redisTemplate.opsForSet()).willReturn(setOperations);
            given(setOperations.members(userSessionKey)).willReturn(new HashSet<>(List.of(mockSessionId)));
            given(setOperations.members(sessionContentKey)).willReturn(new HashSet<>(List.of(contentId.toString())));

            given(watchingSessionRepository.findByContentIdAndUserId(contentId, userId))
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
        @DisplayName("레디스 추적엔 성공했으나 RDB 상에 정합성이 깨져 세션 데이터가 없으면 empty를 반환한다.")
        void redisSessionHitButRdbMiss_returnsEmpty() {
            UUID currentUserId = UUID.randomUUID();
            String userSessionKey = "ws:user:" + userId + ":sessions";
            String mockSessionId = "mock-stomp-session-id";
            String sessionContentKey = "ws:session:" + mockSessionId + ":contents";

            // given
            given(redisTemplate.opsForSet()).willReturn(setOperations);
            given(setOperations.members(userSessionKey)).willReturn(new HashSet<>(List.of(mockSessionId)));
            given(setOperations.members(sessionContentKey)).willReturn(new HashSet<>(List.of(contentId.toString())));

            given(watchingSessionRepository.findByContentIdAndUserId(contentId, userId))
                    .willReturn(Optional.empty());

            // when
            Optional<WatchingSessionDto> result =
                    watchingSessionService.findCurrentWatchingSessionByUserId(userId, currentUserId);

            // then
            assertThat(result).isEmpty();
        }
    }
}
