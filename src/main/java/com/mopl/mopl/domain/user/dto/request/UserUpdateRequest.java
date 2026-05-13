package com.mopl.mopl.domain.user.dto.request;

import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
        @Size(max = 50)
        String name
) {
}