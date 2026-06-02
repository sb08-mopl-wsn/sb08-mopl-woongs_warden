package com.mopl.mopl.global.event.listener;

import com.mopl.mopl.domain.notification.dto.NotificationDto;
import com.mopl.mopl.domain.notification.entity.Notification;
import com.mopl.mopl.domain.notification.entity.NotificationLevel;
import com.mopl.mopl.domain.notification.mapper.NotificationMapper;
import com.mopl.mopl.domain.notification.repository.NotificationRepository;
import com.mopl.mopl.domain.user.entity.Role;
import com.mopl.mopl.domain.user.entity.User;
import com.mopl.mopl.domain.user.repository.UserRepository;
import com.mopl.mopl.global.event.user.UserEvent;
import com.mopl.mopl.global.event.user.UserPasswordInitEvent;
import com.mopl.mopl.global.event.user.UserUpdateLockEvent;
import com.mopl.mopl.global.event.user.UserUpdateProfileEvent;
import com.mopl.mopl.global.event.user.UserUpdateRoleEvent;
import com.mopl.mopl.global.mail.MailService;
import com.mopl.mopl.global.sse.service.SseService;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserEventListenerTest {

    @Mock
    private SseService sseService;

    @Mock
    private MailService mailService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationMapper notificationMapper;

    @InjectMocks
    private UserEventListener userEventListener;

    @Test
    @DisplayName("사용자 생성 이벤트 발생 시 알림을 저장하고 SSE 알림을 전송한다")
    void onUserCreated_savesNotificationAndSendsNotification() {
        UUID userId = UUID.randomUUID();
        UserEvent event = new UserEvent(userId, "홍길동");
        User user = User.builder().name("홍길동").build();
        Notification savedNotification = Notification.builder()
                .user(user)
                .title("회원가입을 환영합니다")
                .content("환영합니다! 홍길동님")
                .level(NotificationLevel.INFO)
                .build();
        NotificationDto dto = new NotificationDto(UUID.randomUUID(), "회원가입을 환영합니다", "환영합니다! 홍길동님", NotificationLevel.INFO, userId, null);

        when(userRepository.getReferenceById(userId)).thenReturn(user);
        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);
        when(notificationMapper.toDto(savedNotification)).thenReturn(dto);

        userEventListener.onUserCreated(event);

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());
        Notification notification = notificationCaptor.getValue();
        assertThat(notification.getUser()).isEqualTo(user);
        assertThat(notification.getTitle()).isEqualTo("회원가입을 환영합니다");
        assertThat(notification.getContent()).isEqualTo("환영합니다! 홍길동님");
        assertThat(notification.getLevel()).isEqualTo(NotificationLevel.INFO);
        verify(sseService).sendNotification(userId, dto);
    }

    @Test
    @DisplayName("사용자 생성 알림 전송 실패 시 예외를 전파하지 않는다")
    void onUserCreated_whenNotificationFails_doesNotThrow() {
        UUID userId = UUID.randomUUID();
        UserEvent event = new UserEvent(userId, "홍길동");
        User user = User.builder().name("홍길동").build();
        Notification savedNotification = Notification.builder()
                .user(user)
                .title("회원가입을 환영합니다")
                .content("환영합니다! 홍길동님")
                .level(NotificationLevel.INFO)
                .build();
        NotificationDto dto = new NotificationDto(UUID.randomUUID(), "회원가입을 환영합니다", "환영합니다! 홍길동님", NotificationLevel.INFO, userId, null);

        when(userRepository.getReferenceById(userId)).thenReturn(user);
        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);
        when(notificationMapper.toDto(savedNotification)).thenReturn(dto);
        doThrow(new RuntimeException("SSE 실패"))
                .when(sseService)
                .sendNotification(eq(userId), eq(dto));

        userEventListener.onUserCreated(event);

        verify(notificationRepository).save(any(Notification.class));
        verify(sseService).sendNotification(userId, dto);
    }

    @Test
    @DisplayName("사용자 잠금 이벤트 발생 시 잠금 안내 메일을 전송한다")
    void onUserLock_sendsMail() {
        UUID userId = UUID.randomUUID();
        UserUpdateLockEvent event = mockUserUpdateLockEvent(userId);

        userEventListener.onUserLock(event);

        verify(mailService).userLockedUpdate("test@example.com", true, "홍길동");
    }

    @Test
    @DisplayName("비밀번호 초기화 이벤트 발생 시 임시 비밀번호 메일을 전송한다")
    void onUserPasswordInit_sendsMail() {
        UUID userId = UUID.randomUUID();
        Instant expiredAt = Instant.now().plusSeconds(1800);
        UserPasswordInitEvent event = mock(UserPasswordInitEvent.class);

        when(event.userId()).thenReturn(userId);
        when(event.username()).thenReturn("홍길동");
        when(event.email()).thenReturn("test@example.com");
        when(event.password()).thenReturn("Temp1234!");
        when(event.expiredAt()).thenReturn(expiredAt);

        userEventListener.onUserPasswordInit(event);

        verify(mailService).sendInitPassword("test@example.com", "Temp1234!", expiredAt);
    }

    @Test
    @DisplayName("권한 변경 이벤트 발생 시 SSE 알림을 전송한다")
    void onUserRoleUpdate_sendsNotification() {
        UUID userId = UUID.randomUUID();
        UserUpdateRoleEvent event = mock(UserUpdateRoleEvent.class);

        when(event.userId()).thenReturn(userId);
        when(event.name()).thenReturn("홍길동");
        when(event.role()).thenReturn(Role.ADMIN);

        userEventListener.onUserRoleUpdate(event);

        verify(sseService).sendNotification(
                userId,
                "홍길동님의 권한이 ADMIN로 변경되었습니다."
        );
    }

    @Test
    @DisplayName("프로필 변경 이벤트 발생 시 알림을 저장하고 SSE 알림을 전송한다")
    void onUserProfileUpdate_savesNotificationAndSendsNotification() {
        UUID userId = UUID.randomUUID();
        UserUpdateProfileEvent event = new UserUpdateProfileEvent(userId, "홍길동");
        User user = User.builder().name("홍길동").build();
        Notification savedNotification = Notification.builder()
                .user(user)
                .title("프로필 변경 안내")
                .content("홍길동님의 프로필이 변경됐어요.")
                .level(NotificationLevel.INFO)
                .build();
        NotificationDto dto = new NotificationDto(UUID.randomUUID(), "프로필 변경 안내", "홍길동님의 프로필이 변경됐어요.", NotificationLevel.INFO, userId, null);

        when(userRepository.getReferenceById(userId)).thenReturn(user);
        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);
        when(notificationMapper.toDto(savedNotification)).thenReturn(dto);

        userEventListener.onUserProfileUpdate(event);

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());
        Notification notification = notificationCaptor.getValue();
        assertThat(notification.getUser()).isEqualTo(user);
        assertThat(notification.getTitle()).isEqualTo("프로필 변경 안내");
        assertThat(notification.getContent()).isEqualTo("홍길동님의 프로필이 변경됐어요.");
        assertThat(notification.getLevel()).isEqualTo(NotificationLevel.INFO);
        verify(sseService).sendNotification(userId, dto);
    }

    private UserUpdateLockEvent mockUserUpdateLockEvent(UUID userId) {
        UserUpdateLockEvent event = mock(UserUpdateLockEvent.class);
        when(event.userId()).thenReturn(userId);
        when(event.name()).thenReturn("홍길동");
        when(event.userEmail()).thenReturn("test@example.com");
        when(event.isLocked()).thenReturn(true);
        return event;
    }
}