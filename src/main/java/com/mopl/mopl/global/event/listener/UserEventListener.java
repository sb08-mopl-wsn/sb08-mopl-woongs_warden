package com.mopl.mopl.global.event.listener;

import com.mopl.mopl.global.config.AsyncConfig;
import com.mopl.mopl.global.event.user.UserEvent;
import com.mopl.mopl.global.event.user.UserPasswordInitEvent;
import com.mopl.mopl.global.event.user.UserUpdateLockEvent;
import com.mopl.mopl.global.event.user.UserUpdateRoleEvent;
import com.mopl.mopl.global.mail.MailService;
import com.mopl.mopl.global.sse.service.SseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventListener {
    private final SseService sseService;
    private final MailService mailService;

    @Async(AsyncConfig.USER_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserCreated(UserEvent event) {
        log.info("[UserEvent] 생성 - name: {}, id: {}", event.name(), event.userId());

        // SSE 푸시 알림 발송
        try {
            sseService.sendNotification(
                    event.userId(),
                    "환영합니다! " + event.name() + "님"
            );
        } catch (Exception e) {
            log.warn("생성 알림 전송 실패 - name: {}, id: {}", event.name(), event.userId(), e);
        }
    }

    @Async(AsyncConfig.USER_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserLock(UserUpdateLockEvent event) {
        log.info("[UserEvent] 정지관련 이벤트 발생 - name: {}, id: {}", event.name(), event.userId());

        // SSE 푸시 알림 발송
        try {
            mailService.userLockedUpdate(
                    event.userEmail(),
                    event.isLocked(),
                    event.name()
            );

        } catch (Exception e) {
            log.warn("메일 전송 실패 - name: {}, id: {}", event.name(), event.userId(), e);
        }
    }

    @Async(AsyncConfig.USER_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserPasswordInit(UserPasswordInitEvent event) {
        log.info("[UserEvent] 비밀번호 초기화 이벤트 발생 - name: {}, id: {}", event.username(), event.userId());

        // SSE 푸시 알림 발송
        try {
            mailService.sendInitPassword(
                    event.email(),
                    event.password(),
                    event.expiredAt()
            );

        } catch (Exception e) {
            log.warn("메일 전송 실패 - name: {}, id: {}", event.username(), event.userId(), e);
        }
    }

    @Async(AsyncConfig.USER_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserRoleUpdate(UserUpdateRoleEvent event) {
        log.warn("[UserEvent] 권한 변경 - name: {}, id: {}, 권한: {}", event.name(), event.userId(), event.role());

        // SSE 푸시 알림 발송
        try {
            sseService.sendNotification(
                    event.userId(),
                    event.name() + "님의 권한이 " + event.role() + "로 변경되었습니다."
            );
        } catch (Exception e) {
            log.warn("권한 변경 알림 전송 실패 - name: {}, id: {}", event.name(), event.userId(), e);
        }
    }

    @Async(AsyncConfig.USER_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserProfileUpdate(UserEvent event) {
        log.info("[UserEvent] 프로필 변경 - name: {}, id: {}", event.name(), event.userId());

        // SSE 푸시 알림 발송
        try {
            sseService.sendNotification(
                    event.userId(),
                    event.name() + "님의 프로필이 변경됐어요."
            );
        } catch (Exception e) {
            log.warn("프로필 알림 전송 실패 - name: {}, id: {}", event.name(), event.userId(), e);
        }
    }
}