package com.mopl.mopl.domain.auth;

import com.mopl.mopl.domain.auth.service.AuthenticationAttemptService;
import com.mopl.mopl.global.exception.oauth2.LoginAttemptLockedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthenticationAttemptServiceTest {

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);

    private final AuthenticationAttemptService service =
            new AuthenticationAttemptService(redisTemplate);

    @Test
    @DisplayName("로그인 잠금 키가 없으면 validateLoginAvailable은 예외를 던지지 않는다")
    void validateLoginAvailable_notLocked() {
        String email = "USER@Test.COM";

        when(redisTemplate.hasKey("auth:lock:login:user@test.com"))
                .thenReturn(false);

        assertThatCode(() -> service.validateLoginAvailable(email))
                .doesNotThrowAnyException();

        verify(redisTemplate).hasKey("auth:lock:login:user@test.com");
    }

    @Test
    @DisplayName("로그인 잠금 키가 있으면 LoginAttemptLockedException을 던진다")
    void validateLoginAvailable_locked() {
        String email = " user@test.com ";

        when(redisTemplate.hasKey("auth:lock:login:user@test.com"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.validateLoginAvailable(email))
                .isInstanceOf(LoginAttemptLockedException.class)
                .hasMessage("비밀번호를 5회 틀려 30분간 로그인이 제한됩니다.");

        verify(redisTemplate).hasKey("auth:lock:login:user@test.com");
    }

    @Test
    @DisplayName("username이 null이면 validateLoginAvailable은 Redis를 조회하지 않는다")
    void validateLoginAvailable_nullUsername() {
        assertThatCode(() -> service.validateLoginAvailable(null))
                .doesNotThrowAnyException();

        verifyNoInteractions(redisTemplate);
    }

    @Test
    @DisplayName("username이 blank이면 validateLoginAvailable은 Redis를 조회하지 않는다")
    void validateLoginAvailable_blankUsername() {
        assertThatCode(() -> service.validateLoginAvailable("   "))
                .doesNotThrowAnyException();

        verifyNoInteractions(redisTemplate);
    }

    @Test
    @DisplayName("로그인 실패 기록 결과가 0이면 false를 반환한다")
    void recordLoginFailure_notLocked() {
        String email = "USER@Test.COM";

        when(redisTemplate.execute(
                any(RedisScript.class),
                eq(List.of(
                        "auth:attempt:login:user@test.com",
                        "auth:lock:login:user@test.com"
                )),
                eq("1800000"),
                eq("5"),
                eq("1800000")
        )).thenReturn(0L);

        boolean result = service.recordLoginFailure(email);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("로그인 실패 기록 결과가 1이면 true를 반환한다")
    void recordLoginFailure_locked() {
        String email = " user@test.com ";

        when(redisTemplate.execute(
                any(RedisScript.class),
                eq(List.of(
                        "auth:attempt:login:user@test.com",
                        "auth:lock:login:user@test.com"
                )),
                eq("1800000"),
                eq("5"),
                eq("1800000")
        )).thenReturn(1L);

        boolean result = service.recordLoginFailure(email);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("로그인 실패 기록 username이 null이면 false를 반환하고 Redis를 호출하지 않는다")
    void recordLoginFailure_nullUsername() {
        boolean result = service.recordLoginFailure(null);

        assertThat(result).isFalse();
        verifyNoInteractions(redisTemplate);
    }

    @Test
    @DisplayName("로그인 실패 기록 username이 blank이면 false를 반환하고 Redis를 호출하지 않는다")
    void recordLoginFailure_blankUsername() {
        boolean result = service.recordLoginFailure("   ");

        assertThat(result).isFalse();
        verifyNoInteractions(redisTemplate);
    }

    @Test
    @DisplayName("로그인 실패 횟수 초기화는 attempt 키와 lock 키를 삭제한다")
    void resetLoginFailures() {
        String email = " USER@Test.COM ";

        service.resetLoginFailures(email);

        verify(redisTemplate).delete(List.of(
                "auth:attempt:login:user@test.com",
                "auth:lock:login:user@test.com"
        ));
    }

    @Test
    @DisplayName("로그인 실패 횟수 초기화 username이 null이면 Redis를 호출하지 않는다")
    void resetLoginFailures_nullUsername() {
        service.resetLoginFailures(null);

        verifyNoInteractions(redisTemplate);
    }

    @Test
    @DisplayName("비밀번호 초기화 잠금 키가 있으면 true를 반환한다")
    void isPasswordResetLocked_true() {
        String email = "USER@Test.COM";

        when(redisTemplate.hasKey("auth:lock:password-reset:user@test.com"))
                .thenReturn(true);

        boolean result = service.isPasswordResetLocked(email);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("비밀번호 초기화 잠금 키가 없으면 false를 반환한다")
    void isPasswordResetLocked_false() {
        String email = "USER@Test.COM";

        when(redisTemplate.hasKey("auth:lock:password-reset:user@test.com"))
                .thenReturn(false);

        boolean result = service.isPasswordResetLocked(email);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("비밀번호 초기화 실패 기록 결과가 1이면 true를 반환한다")
    void recordPasswordResetFailure_locked() {
        String email = "USER@Test.COM";

        when(redisTemplate.execute(
                any(RedisScript.class),
                eq(List.of(
                        "auth:attempt:password-reset:user@test.com",
                        "auth:lock:password-reset:user@test.com"
                )),
                eq("1800000"),
                eq("5"),
                eq("1800000")
        )).thenReturn(1L);

        boolean result = service.recordPasswordResetFailure(email);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("비밀번호 초기화 실패 횟수 초기화는 attempt 키와 lock 키를 삭제한다")
    void resetPasswordResetFailures() {
        String email = " USER@Test.COM ";

        service.resetPasswordResetFailures(email);

        verify(redisTemplate).delete(List.of(
                "auth:attempt:password-reset:user@test.com",
                "auth:lock:password-reset:user@test.com"
        ));
    }
}