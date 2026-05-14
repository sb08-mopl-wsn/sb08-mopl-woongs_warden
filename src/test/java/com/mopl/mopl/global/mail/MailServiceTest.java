package com.mopl.mopl.global.mail;

import com.mopl.mopl.global.exception.mail.MailFailedLoadException;
import com.mopl.mopl.global.exception.mail.MailFailedSendException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.Instant;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MailServiceTest {

    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final MailService mailService = new MailService(mailSender);

    @Test
    @DisplayName("임시 비밀번호 메일 전송 성공")
    void sendInitPassword_success() throws Exception {
        // given
        MimeMessage message = new MimeMessage(Session.getDefaultInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);

        String to = "test@example.com";
        String rawPassword = "Temp1234!";
        Instant expiredAt = Instant.parse("2026-05-13T01:30:00Z");

        // when
        mailService.sendInitPassword(to, rawPassword, expiredAt);

        // then
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(message);
    }

    @Test
    @DisplayName("계정 정지 메일 전송 성공")
    void userLockedUpdate_lock_success() throws Exception {
        // given
        MimeMessage message = new MimeMessage(Session.getDefaultInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);

        // when
        mailService.userLockedUpdate("test@example.com", true, "홍길동");

        // then
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(message);
    }

    @Test
    @DisplayName("계정 정지 해제 메일 전송 성공")
    void userLockedUpdate_unlock_success() throws Exception {
        // given
        MimeMessage message = new MimeMessage(Session.getDefaultInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);

        // when
        mailService.userLockedUpdate("test@example.com", false, "홍길동");

        // then
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(message);
    }

    @Test
    @DisplayName("계정 정지 메일 전송 실패 시 MailFailedSendException 발생")
    void userLockedUpdate_sendFail_throwMailFailedSendException() throws Exception {
        // given
        MimeMessage message = new MimeMessage(Session.getDefaultInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);

        doThrow(new MailSendException("send fail"))
                .when(mailSender)
                .send(any(MimeMessage.class));

        // when & then
        assertThatThrownBy(() ->
                mailService.userLockedUpdate(
                        "test@example.com",
                        true,
                        "홍길동"
                )
        ).isInstanceOf(MailFailedSendException.class);
    }
}