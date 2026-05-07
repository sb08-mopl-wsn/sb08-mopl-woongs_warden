package com.mopl.mopl.domain.user.dto.request;

import com.mopl.mopl.domain.user.entity.Role;

public record UserRoleUpdateRequest(
        Role role
) {
}
