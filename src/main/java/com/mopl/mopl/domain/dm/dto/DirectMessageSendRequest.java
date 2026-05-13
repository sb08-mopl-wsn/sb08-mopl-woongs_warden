package com.mopl.mopl.domain.dm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DirectMessageSendRequest(
    @NotBlank(message = "메시지 내용은 비어있을 수 없습니다.")
    @Size(max = 2000, message = "메시지는 최대 2000자까지 입력 가능합니다.")
    String content
) {

}
