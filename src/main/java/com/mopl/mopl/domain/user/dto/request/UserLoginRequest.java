package com.mopl.mopl.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UserLoginRequest(
        @NotBlank(message = "이름은 필수 입력값입니다.")
        String username,

        @NotBlank(message = "비밀번호는 필수 입력값입니다.")
        String password,

        Boolean rememberMe
) {
}