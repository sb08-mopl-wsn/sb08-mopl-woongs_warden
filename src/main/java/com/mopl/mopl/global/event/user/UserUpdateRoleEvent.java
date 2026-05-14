package com.mopl.mopl.global.event.user;
import com.mopl.mopl.domain.user.entity.Role;
import com.mopl.mopl.domain.user.entity.User;

import java.util.UUID;

public record UserUpdateRoleEvent(
        UUID userId,
        String name,
        Role role
) {
    public static UserUpdateRoleEvent of(User user) {
        return new UserUpdateRoleEvent(
                user.getId(),
                user.getName(),
                user.getRole()
        );
    }
}
