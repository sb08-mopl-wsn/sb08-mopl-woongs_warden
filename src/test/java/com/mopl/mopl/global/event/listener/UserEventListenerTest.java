package com.mopl.mopl.global.event.listener;

import com.mopl.mopl.domain.user.entity.Role;
import com.mopl.mopl.global.event.user.UserEvent;
import com.mopl.mopl.global.event.user.UserPasswordInitEvent;
import com.mopl.mopl.global.event.user.UserUpdateLockEvent;
import com.mopl.mopl.global.event.user.UserUpdateRoleEvent;
import com.mopl.mopl.global.mail.MailService;
import com.mopl.mopl.global.sse.service.SseService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserEventListenerTest {

    @Mock
    private SseService sseService;

    @Mock
    private MailService mailService;

    @InjectMocks
    private UserEventListener userEventListener;

    @Test
    @DisplayName("사용자 생성 이벤트 발생 시 환영 SSE 알림을 전송한다")
    void onUserCreated_sendsNotification() {
        UUID userId = UUID.randomUUID();
        UserEvent event = mock(UserEvent.class);

        when(event.userId()).thenReturn(userId);
        when(event.name()).thenReturn("홍길동");

        userEventListener.onUserCreated(event);

        verify(sseService).sendNotification(userId, "환영합니다! 홍길동님");
    }

    @Test
    @DisplayName("사용자 생성 알림 전송 실패 시 예외를 전파하지 않는다")
    void onUserCreated_whenNotificationFails_doesNotThrow() {
        UUID userId = UUID.randomUUID();
        UserEvent event = mock(UserEvent.class);

        when(event.userId()).thenReturn(userId);
        when(event.name()).thenReturn("홍길동");
        doThrow(new RuntimeException("SSE 실패"))
                .when(sseService)
                .sendNotification(any(), anyString());

        userEventListener.onUserCreated(event);

        verify(sseService).sendNotification(userId, "환영합니다! 홍길동님");
    }

    @Test
    @DisplayName("사용자 잠금 이벤트 발생 시 잠금 안내 메일을 전송한다")
    void onUserLock_sendsMail() {
        UUID userId = UUID.randomUUID();
        UserUpdateLockEvent event = mock(UserUpdateLockEvent.class);

        when(event.userId()).thenReturn(userId);
        when(event.name()).thenReturn("홍길동");
        when(event.userEmail()).thenReturn("test@example.com");
        when(event.isLocked()).thenReturn(true);

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
    @DisplayName("프로필 변경 이벤트 발생 시 SSE 알림을 전송한다")
    void onUserProfileUpdate_sendsNotification() {
        UUID userId = UUID.randomUUID();
        UserEvent event = mock(UserEvent.class);

        when(event.userId()).thenReturn(userId);
        when(event.name()).thenReturn("홍길동");

        userEventListener.onUserProfileUpdate(event);

        verify(sseService).sendNotification(
                userId,
                "홍길동님의 프로필이 변경됐어요."
        );
    }
}