package com.mopl.mopl.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UserSummary(
        @NotNull(message = "사용자 ID는 필수입니다")
        UUID userId,
        @NotBlank(message = "사용자 이름은 필수입니다")
        String name,
        String profileImageUrl
) {
}