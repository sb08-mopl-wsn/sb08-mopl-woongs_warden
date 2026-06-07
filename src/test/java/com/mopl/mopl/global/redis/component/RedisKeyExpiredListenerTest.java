package com.mopl.mopl.global.redis.component;

import com.mopl.mopl.domain.notification.service.kafka.BadWordNotificationProcessor;
import com.mopl.mopl.domain.user.entity.User;
import com.mopl.mopl.domain.user.repository.UserRepository;
import com.mopl.mopl.global.component.UserUnbanProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RedisKeyExpiredListener 테스트")
public class RedisKeyExpiredListenerTest {

    @Mock
    private RedisMessageListenerContainer listenerContainer;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserUnbanProcessor userUnbanProcessor;

    @Mock
    private Message mockMessage;

    private RedisKeyExpiredListener redisKeyExpiredListener;
    private UUID userId;
    private User mockUser;

    @BeforeEach
    void setUp() {
        redisKeyExpiredListener = new RedisKeyExpiredListener(
                listenerContainer,
                userRepository,
                userUnbanProcessor
        );

        userId = UUID.randomUUID();
        mockUser = User.builder()
                .name("재준")
                .email("user@codeit.com")
                .build();
    }

    @Test
    @DisplayName("만료된 Redis 키가 벤 프리픽스로 시작하면, 키에서 userId를 추출하여 유저의 정지를 해제(processUnban)한다.")
    void onMessage_validBanKey_unbansUser() {
        // given
        String expiredKey = BadWordNotificationProcessor.BAN_KEY_PREFIX + userId.toString();
        given(mockMessage.toString()).willReturn(expiredKey);

        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));

        // when
        redisKeyExpiredListener.onMessage(mockMessage, new byte[0]);

        // then
        verify(userRepository, times(1)).findById(userId);
        verify(userUnbanProcessor, times(1)).processUnban(mockUser);
    }

    @Test
    @DisplayName("만료된 Redis 키가 벤 프리픽스로 시작하지 않으면, 비즈니스 로직을 타지 않고 즉시 스킵한다.")
    void onMessage_otherKey_skipsProcessing() {
        // given
        String otherKey = "content:watcher:count:" + UUID.randomUUID().toString();
        given(mockMessage.toString()).willReturn(otherKey);

        // when
        redisKeyExpiredListener.onMessage(mockMessage, new byte[0]);

        // then
        verifyNoInteractions(userRepository, userUnbanProcessor);
    }

    @Test
    @DisplayName("만료된 키에 해당하는 유저가 DB에 존재하지 않으면, 해제 프로세서를 호출하지 않고 안전하게 종료한다.")
    void onMessage_userNotFound_doesNotUnban() {
        // given
        String expiredKey = BadWordNotificationProcessor.BAN_KEY_PREFIX + userId.toString();
        given(mockMessage.toString()).willReturn(expiredKey);

        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when
        redisKeyExpiredListener.onMessage(mockMessage, new byte[0]);

        // then
        verify(userRepository, times(1)).findById(userId);
        verifyNoInteractions(userUnbanProcessor);
    }

    @Test
    @DisplayName("정지 해제 처리 중 예외(UUID 파싱 에러, DB 에러 등)가 발생하더라도 에러를 catch 블록에서 격리하여 수신 파이프라인을 유지한다.")
    void onMessage_exceptionThrown_isolatedInCatchBlock() {
        // given
        String corruptedKey = BadWordNotificationProcessor.BAN_KEY_PREFIX + "corrupted-user-id-string";
        given(mockMessage.toString()).willReturn(corruptedKey);

        // when & then
        redisKeyExpiredListener.onMessage(mockMessage, new byte[0]);
        verifyNoInteractions(userRepository, userUnbanProcessor);
    }
}