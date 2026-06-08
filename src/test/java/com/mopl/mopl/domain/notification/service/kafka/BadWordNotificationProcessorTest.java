package com.mopl.mopl.domain.notification.service.kafka;

import com.mopl.mopl.domain.jwt.registry.JwtRegistry;
import com.mopl.mopl.domain.notification.dto.NotificationDto;
import com.mopl.mopl.domain.notification.entity.Notification;
import com.mopl.mopl.domain.notification.mapper.NotificationMapper;
import com.mopl.mopl.domain.notification.repository.NotificationRepository;
import com.mopl.mopl.domain.user.entity.User;
import com.mopl.mopl.domain.user.exception.UserNotFoundException;
import com.mopl.mopl.domain.user.repository.UserRepository;
import com.mopl.mopl.global.event.BadWordDetectedEvent;
import com.mopl.mopl.global.sse.service.SseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BadWordNotificationProcessorTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private NotificationMapper notificationMapper;
    @Mock
    private SseService sseService;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;
    @Mock
    private JwtRegistry jwtRegistry;

    @InjectMocks
    private BadWordNotificationProcessor badWordNotificationProcessor;

    private UUID userId;
    private User mockUser;
    private Notification mockNotification;
    private NotificationDto mockDto;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        mockUser = User.builder()
                .email("user@test.com")
                .name("test")
                .build();
        ReflectionTestUtils.setField(mockUser, "id", userId);
        ReflectionTestUtils.setField(mockUser, "warningCount", 0);
        ReflectionTestUtils.setField(mockUser, "isBanned", false);
        ReflectionTestUtils.setField(mockUser, "isLocked", false);

        mockNotification = Notification.builder()
                .user(mockUser)
                .title("Test")
                .content("Content")
                .build();
        mockDto = new NotificationDto(
                UUID.randomUUID(),
                "Test",
                "Content",
                null,
                userId,
                null
        );
    }

    @Test
    @DisplayName("존재하지 않는 유저 ID의 비속어 이벤트 수신 시 UserNotFoundException이 발생한다.")
    void processBadWordDetected_UserNotFound() {
        // given
        BadWordDetectedEvent event = new BadWordDetectedEvent(userId, "바보");
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> badWordNotificationProcessor.processBadWordDetected(event))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("단순 경고 상태일 때, 경고 횟수가 1 증가하고 단순 경고용 알림이 생성 및 발송된다.")
    void processBadWordDetected_JustWarning() {
        // given
        BadWordDetectedEvent event = new BadWordDetectedEvent(userId, "바보");
        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));

        given(notificationRepository.saveAndFlush(any(Notification.class))).willReturn(mockNotification);
        given(notificationMapper.toDto(any())).willReturn(mockDto);

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);

        // when
        badWordNotificationProcessor.processBadWordDetected(event);

        // then
        assertThat(mockUser.getWarningCount()).isEqualTo(1);

        verify(notificationRepository).saveAndFlush(notificationCaptor.capture());
        Notification actual = notificationCaptor.getValue();

        assertThat(actual.getTitle()).contains("🚨 욕설 사용으로 인한 경고", "[누적 경고: 1]");
        assertThat(actual.getContent()).contains("바른 언어 사용을 부탁드립니다.");
        verify(sseService).sendNotification(eq(userId), eq(mockDto));
        verify(jwtRegistry, never()).invalidateJwtInformationByUserId(any());
    }

    @Test
    @DisplayName("실시간 알림 발송(SSE) 중 네트워크 등 예외가 발생하더라도 메인 비즈니스 트랜잭션은 차단되지 않고 정상 마감된다.")
    void processBadWordDetected_SseThrowsException_maintainsTransaction() throws Exception {
        // given
        BadWordDetectedEvent event = new BadWordDetectedEvent(userId, "바보");
        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
        given(notificationRepository.saveAndFlush(any(Notification.class))).willReturn(mockNotification);
        given(notificationMapper.toDto(any(Notification.class))).willReturn(mockDto);

        // SSE 전송 실패 유도
        doThrow(new RuntimeException("SSE 연결 끊김")).when(sseService).sendNotification(any(), any());

        // when & then (예외가 밖으로 터지지 않는지 검증)
        badWordNotificationProcessor.processBadWordDetected(event);

        assertThat(mockUser.getWarningCount()).isEqualTo(1);
        verify(notificationRepository, times(1)).saveAndFlush(any());
    }

    @Test
    @DisplayName("누적 경고로 인해 1차 정지 상태가 될 때(경고 3회), 5분 이용 금지 안내 알림이 생성된다.")
    void processBadWordDetected_FirstBan() {
        // given
        ReflectionTestUtils.setField(mockUser, "warningCount", 2);

        ReflectionTestUtils.setField(mockUser, "isBanned", true);
        LocalDateTime futureExpiresAt = LocalDateTime.now().plusMinutes(5);
        ReflectionTestUtils.setField(mockUser, "banExpiresAt", futureExpiresAt);

        BadWordDetectedEvent event = new BadWordDetectedEvent(userId, "바보");
        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
        given(notificationRepository.saveAndFlush(any(Notification.class))).willReturn(mockNotification);

        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);

        // when
        badWordNotificationProcessor.processBadWordDetected(event);

        // then
        assertThat(mockUser.getWarningCount()).isEqualTo(3);
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture());
        Notification actual = notificationCaptor.getValue();

        assertThat(actual.getTitle()).contains("🚫 [1차 정지]", "실시간 채팅 5분 이용 금지");
        assertThat(actual.getContent()).contains("5분간 실시간 채팅 이용이 제한됩니다.");

        String expectedRedisKey = "users:banned:ttl:" + userId.toString();
        verify(valueOperations, times(1)).set(eq(expectedRedisKey), eq("banned"), anyLong(), eq(TimeUnit.SECONDS));
        verify(jwtRegistry).invalidateJwtInformationByUserId(userId);
    }

    @Test
    @DisplayName("Redis 인프라 장애로 인해 TTL 설정 연산 도중 예외가 발생하더라도, 메인 로직 트랜잭션은 차단되지 않고 격리 마감된다.")
    void processBadWordDetected_RedisSetThrowsException_IsolatesError() {
        // given
        ReflectionTestUtils.setField(mockUser, "warningCount", 2);
        ReflectionTestUtils.setField(mockUser, "isBanned", true);
        ReflectionTestUtils.setField(mockUser, "banExpiresAt", LocalDateTime.now().plusMinutes(5));

        BadWordDetectedEvent event = new BadWordDetectedEvent(userId, "바보");
        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
        given(notificationRepository.saveAndFlush(any(Notification.class))).willReturn(mockNotification);

        // Redis 장애 주입
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        doThrow(new RuntimeException("Redis 커넥션 끊김")).when(valueOperations).set(anyString(), any(), anyLong(), any());

        // when & then
        badWordNotificationProcessor.processBadWordDetected(event);

        assertThat(mockUser.getWarningCount()).isEqualTo(3);
        verify(notificationRepository, times(1)).saveAndFlush(any());
    }

    @Test
    @DisplayName("정지 만료 시간 격차가 최대 제한치(30일)를 초과할 경우, 가드 조건에 걸려 Redis 설정을 건너뛴다.")
    void processBadWordDetected_BanDurationExceedsMax_SkipsRedisSet() {
        // given
        ReflectionTestUtils.setField(mockUser, "warningCount", 2);
        ReflectionTestUtils.setField(mockUser, "isBanned", true);

        LocalDateTime exceedExpiresAt = LocalDateTime.now().plusDays(31);
        ReflectionTestUtils.setField(mockUser, "banExpiresAt", exceedExpiresAt);

        BadWordDetectedEvent event = new BadWordDetectedEvent(userId, "바보");
        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
        given(notificationRepository.saveAndFlush(any(Notification.class))).willReturn(mockNotification);

        // when
        badWordNotificationProcessor.processBadWordDetected(event);

        // then
        assertThat(mockUser.getWarningCount()).isEqualTo(3);
        verify(valueOperations, never()).set(anyString(), any(), anyLong(), any());
    }

    @Test
    @DisplayName("누적 경고로 인해 2차 정지 상태가 될 때(경고 6회), 1시간 이용 금지 안내 알림이 생성된다.")
    void processBadWordDetected_SecondBan() {
        // given
        ReflectionTestUtils.setField(mockUser, "warningCount", 5);
        ReflectionTestUtils.setField(mockUser, "isBanned", true);
        LocalDateTime futureExpiresAt = LocalDateTime.now().plusHours(1);
        ReflectionTestUtils.setField(mockUser, "banExpiresAt", futureExpiresAt);

        BadWordDetectedEvent event = new BadWordDetectedEvent(userId, "바보");
        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
        given(notificationRepository.saveAndFlush(any(Notification.class))).willReturn(mockNotification);

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);

        // when
        badWordNotificationProcessor.processBadWordDetected(event);

        // then
        assertThat(mockUser.getWarningCount()).isEqualTo(6);
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture());
        Notification actual = notificationCaptor.getValue();

        assertThat(actual.getTitle()).contains("🚫 [2차 정지]", "실시간 채팅 1시간 이용 금지");
        assertThat(actual.getContent()).contains("1시간 실시간 채팅 이용이 제한됩니다.");
    }

    @Test
    @DisplayName("누적 경고가 3의 배수이나 1, 2차 범위를 넘어서면(경고 9회 이상), 영구 정지 안내 알림이 생성된다.")
    void processBadWordDetected_PermanentBan() {
        // given
        ReflectionTestUtils.setField(mockUser, "warningCount", 8);
        ReflectionTestUtils.setField(mockUser, "isBanned", true);
        ReflectionTestUtils.setField(mockUser, "isLocked", true);

        BadWordDetectedEvent event = new BadWordDetectedEvent(userId, "바보");
        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
        given(notificationRepository.saveAndFlush(any(Notification.class))).willReturn(mockNotification);

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);

        // when
        badWordNotificationProcessor.processBadWordDetected(event);

        // then
        assertThat(mockUser.getWarningCount()).isEqualTo(9);
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture());
        Notification actual = notificationCaptor.getValue();

        assertThat(actual.getTitle()).contains("🔒 [영구 정지]", "모든 활동이 정지됩니다.");
        assertThat(actual.getContent()).contains("모든 서비스 이용이 영구 제한됩니다. 관리자에게 문의하세요.");

        verifyNoInteractions(redisTemplate);
    }
}
