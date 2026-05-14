package com.mopl.mopl.global.event.user;

import com.mopl.mopl.domain.user.entity.User;

import java.util.UUID;

public record UserUpdateProfileEvent(
        UUID userId
) {
    public static UserUpdateProfileEvent of(User user) {
        return new UserUpdateProfileEvent(
                user.getId()
        );
    }
}
