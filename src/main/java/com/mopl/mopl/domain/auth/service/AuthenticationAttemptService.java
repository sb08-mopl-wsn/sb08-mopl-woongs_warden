package com.mopl.mopl.domain.auth.service;

import org.springframework.security.authentication.LockedException;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthenticationAttemptService {
    private static final int MAX_FAILURE_COUNT = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(30);
    private final Map<String, AttemptStatus> loginAttempts = new ConcurrentHashMap<>();
    private final Map<String, AttemptStatus> passwordResetAttempts = new ConcurrentHashMap<>();

    public void validateLoginAvailable(String username) {
        validateAvailable(loginAttempts, username, "비밀번호를 5회 틀려 30분간 로그인이 제한됩니다.");
    }

    public boolean recordLoginFailure(String username) {
        return recordFailure(loginAttempts, username);
    }

    public void resetLoginFailures(String username) {
        resetFailures(loginAttempts, username);
    }

    public boolean isPasswordResetLocked(String email) {
        return isLocked(passwordResetAttempts, email);
    }

    public boolean recordPasswordResetFailure(String email) {
        return recordFailure(passwordResetAttempts, email);
    }

    public void resetPasswordResetFailures(String email) {
        resetFailures(passwordResetAttempts, email);
    }

    private void validateAvailable(Map<String, AttemptStatus> attempts, String key, String message) {
        if (isLocked(attempts, key)) {
            throw new LockedException(message);
        }
    }

    private boolean isLocked(Map<String, AttemptStatus> attempts, String key) {
        String normalizedKey = normalize(key);
        if (normalizedKey == null) {
            return false;
        }

        AttemptStatus status = attempts.get(normalizedKey);
        if (status == null || status.lockedUntil() == null) {
            return false;
        }

        if (status.lockedUntil().isAfter(Instant.now())) {
            return true;
        }

        attempts.remove(normalizedKey);
        return false;
    }

    private boolean recordFailure(Map<String, AttemptStatus> attempts, String key) {
        String normalizedKey = normalize(key);
        if (normalizedKey == null) {
            return false;
        }

        AttemptStatus status = attempts.compute(normalizedKey, (ignored, current) -> {
            Instant now = Instant.now();
            if (current != null && current.lockedUntil() != null && current.lockedUntil().isAfter(now)) {
                return current;
            }

            int failureCount = current == null ? 1 : current.failureCount() + 1;
            Instant lockedUntil = failureCount >= MAX_FAILURE_COUNT ? now.plus(LOCK_DURATION) : null;
            return new AttemptStatus(failureCount, lockedUntil);
        });

        return status != null && status.lockedUntil() != null && status.lockedUntil().isAfter(Instant.now());
    }

    private void resetFailures(Map<String, AttemptStatus> attempts, String key) {
        String normalizedKey = normalize(key);
        if (normalizedKey != null) {
            attempts.remove(normalizedKey);
        }
    }

    private String normalize(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        return key.trim().toLowerCase(Locale.ROOT);
    }

    private record AttemptStatus(int failureCount, Instant lockedUntil) {
    }
}