package com.mopl.mopl.global.event.user;

import com.mopl.mopl.domain.user.entity.User;

import java.util.UUID;

public record UserEvent(
        UUID userId,
        String name
) {
    public static UserEvent of(User user) {
        return new UserEvent(
                user.getId(),
                user.getName()
        );
    }
}
