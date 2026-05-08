package com.mopl.mopl.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "비밀번호는 필수 입력값입니다.")
        @Size(min = 8, max =20, message = "비밀번호는 최소 8자 이상, 20자 미만이어야 합니다.")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,20}$",
                message = "비밀번호는 8~20자여야 하며, 영문 대소문자, 숫자, 특수문자를 적어도 하나씩 포함해야 합니다."
        )
        String password
) {
}
