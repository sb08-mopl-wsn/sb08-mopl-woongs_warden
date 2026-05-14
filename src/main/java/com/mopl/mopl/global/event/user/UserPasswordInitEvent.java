package com.mopl.mopl.global.event.user;

import com.mopl.mopl.domain.user.entity.User;

import java.time.Instant;
import java.util.UUID;

public record UserPasswordInitEvent(
        String username,
        UUID userId,
        String email,
        String password,
        Instant expiredAt
) {
    public static UserPasswordInitEvent of(User user, Instant expiredAt) {
        return new UserPasswordInitEvent(
                user.getName(),
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                expiredAt
        );
    }
}