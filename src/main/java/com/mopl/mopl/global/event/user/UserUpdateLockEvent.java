package com.mopl.mopl.global.event.user;

import com.mopl.mopl.domain.user.entity.User;

import java.util.UUID;

public record UserUpdateLockEvent(
        UUID userId,
        String name,
        boolean isLocked,
        String userEmail
) {
    public static UserUpdateLockEvent of(User user) {
        return new UserUpdateLockEvent(
                user.getId(),
                user.getName(),
                user.isLocked(),
                user.getEmail()
        );
    }
}