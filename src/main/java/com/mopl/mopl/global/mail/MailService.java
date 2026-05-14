package com.mopl.mopl.global.mail;

import com.mopl.mopl.global.exception.mail.MailFailedLoadException;
import com.mopl.mopl.global.exception.mail.MailFailedSendException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;


@Service
@RequiredArgsConstructor
public class MailService {
    private final JavaMailSender mailSender;

    // 초기화된 비밀번호 이메일로 전송
    public void sendInitPassword(String to, String rawPassword, Instant expiredAt) {
        try {
            String html = loadMailTemplate("templates/mail/init-password.html");

            String expiredAtText = DateTimeFormatter
                    .ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.of("Asia/Seoul"))
                    .format(expiredAt);

            html = html.replace("{{TEMP_PASSWORD}}", rawPassword);
            html = html.replace("{{EXPIRED_AT}}", expiredAtText);

            MimeMessage message = mailSender.createMimeMessage();

            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    false,
                    "UTF-8"
            );

            helper.setTo(to);
            helper.setSubject("[MOPL] 임시 비밀번호 안내");
            helper.setText(html, true);

            mailSender.send(message);

        } catch (MessagingException e) {
            throw new MailFailedSendException(e.getMessage());
        }
    }

    // 초기화된 비밀번호 이메일로 전송
    public void userLockedUpdate(String to, boolean lock, String name) {
        try {
            String html = loadMailTemplate("templates/mail/init-password.html");

            String meg = null;
            if (lock) {
                meg = "계정이 정지 되었습니다. 운영자 이메일로 문의해보세요.";
            } else {
                meg = "계정 정지가 해제 되었습니다.";
            }


            html = html.replace("{{meg}}", meg);
            html = html.replace("{{name}}", name);

            MimeMessage message = mailSender.createMimeMessage();

            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    false,
                    "UTF-8"
            );

            helper.setTo(to);
            helper.setSubject("[MOPL] 계정 정지 안내");
            helper.setText(html, true);

            mailSender.send(message);

        } catch (MessagingException e) {
            throw new MailFailedSendException(e.getMessage());
        }
    }

    // 이메일 템플릿 불러오기
    private String loadMailTemplate(String path) {
        try {
            ClassPathResource resource = new ClassPathResource(path);
            try (var in = resource.getInputStream()) {
                byte[] bytes = in.readAllBytes();
                return new String(bytes, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new MailFailedLoadException(e.getMessage());
        }
    }
}
