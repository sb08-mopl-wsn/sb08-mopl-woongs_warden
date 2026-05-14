package com.mopl.mopl.global.event.user;
import com.mopl.mopl.domain.user.entity.Role;

import java.util.UUID;

public record UserUpdateRoleEvent(
        UUID userId,
        String name,
        Role role
) {
}
