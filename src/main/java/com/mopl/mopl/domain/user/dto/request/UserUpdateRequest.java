package com.mopl.mopl.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
    @NotBlank(message = "이름은 필수 입력값입니다.")
    @Size( max = 50)
    String name
) {
}