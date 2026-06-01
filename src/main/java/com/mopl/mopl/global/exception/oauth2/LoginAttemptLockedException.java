package com.mopl.mopl.global.exception.oauth2;
import org.springframework.security.authentication.LockedException;

public class LoginAttemptLockedException extends LockedException {
    public LoginAttemptLockedException(String message) {
        super(message);
    }
}