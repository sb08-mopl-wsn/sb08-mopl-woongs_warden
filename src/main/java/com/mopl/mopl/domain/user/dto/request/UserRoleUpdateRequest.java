package com.mopl.mopl.domain.user.dto.request;

import com.mopl.mopl.domain.user.entity.Role;
import jakarta.validation.constraints.NotNull;

public record UserRoleUpdateRequest(
        @NotNull(message = "role은 필수입니다.")
        Role role
) {
}
