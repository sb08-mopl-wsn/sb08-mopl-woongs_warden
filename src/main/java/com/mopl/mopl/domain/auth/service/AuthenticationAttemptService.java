package com.mopl.mopl.domain.auth.service;

import com.mopl.mopl.global.exception.oauth2.LoginAttemptLockedException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthenticationAttemptService {
    private static final int MAX_FAILURE_COUNT = 5;
    private static final Duration ATTEMPT_TTL = Duration.ofMinutes(30);
    private static final Duration LOCK_DURATION = Duration.ofMinutes(30);
    private static final String ATTEMPT_KEY_PREFIX = "auth:attempt:";
    private static final String LOCK_KEY_PREFIX = "auth:lock:";
    private static final String LOGIN_TYPE = "login";
    private static final String PASSWORD_RESET_TYPE = "password-reset";
    private static final DefaultRedisScript<Long> RECORD_FAILURE_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('EXISTS', KEYS[2]) == 1 then
                return 1
            end

            local failureCount = redis.call('INCR', KEYS[1])
            if failureCount == 1 then
                redis.call('PEXPIRE', KEYS[1], ARGV[1])
            end

            if failureCount >= tonumber(ARGV[2]) then
                redis.call('SET', KEYS[2], '1', 'PX', ARGV[3])
                redis.call('DEL', KEYS[1])
                return 1
            end

            return 0
            """, Long.class);

    private final StringRedisTemplate redisTemplate;

    public void validateLoginAvailable(String username) {
        if (isLocked(LOGIN_TYPE, username)) {
            throw new LoginAttemptLockedException("비밀번호를 5회 틀려 30분간 로그인이 제한됩니다.");
        }
    }

    public boolean recordLoginFailure(String username) {
        return recordFailure(LOGIN_TYPE, username);
    }

    public void resetLoginFailures(String username) {
        resetFailures(LOGIN_TYPE, username);
    }

    public boolean isPasswordResetLocked(String email) {
        return isLocked(PASSWORD_RESET_TYPE, email);
    }

    public boolean recordPasswordResetFailure(String email) {
        return recordFailure(PASSWORD_RESET_TYPE, email);
    }

    public void resetPasswordResetFailures(String email) {
        resetFailures(PASSWORD_RESET_TYPE, email);
    }

    private boolean isLocked(String type, String key) {
        String normalizedKey = normalize(key);
        if (normalizedKey == null) {
            return false;
        }

        Boolean locked = redisTemplate.hasKey(lockKey(type, normalizedKey));
        return Boolean.TRUE.equals(locked);
    }

    private boolean recordFailure(String type, String key) {
        String normalizedKey = normalize(key);
        if (normalizedKey == null) {
            return false;
        }

        Long result = redisTemplate.execute(
                RECORD_FAILURE_SCRIPT,
                List.of(attemptKey(type, normalizedKey), lockKey(type, normalizedKey)),
                String.valueOf(ATTEMPT_TTL.toMillis()),
                String.valueOf(MAX_FAILURE_COUNT),
                String.valueOf(LOCK_DURATION.toMillis())
        );
        return Long.valueOf(1L).equals(result);
    }

    private void resetFailures(String type, String key) {
        String normalizedKey = normalize(key);
        if (normalizedKey == null) {
            return;
        }

        redisTemplate.delete(List.of(attemptKey(type, normalizedKey), lockKey(type, normalizedKey)));
    }

    private String attemptKey(String type, String normalizedKey) {
        return ATTEMPT_KEY_PREFIX + type + ":" + normalizedKey;
    }

    private String lockKey(String type, String normalizedKey) {
        return LOCK_KEY_PREFIX + type + ":" + normalizedKey;
    }

    private String normalize(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        return key.trim().toLowerCase(Locale.ROOT);
    }
}
