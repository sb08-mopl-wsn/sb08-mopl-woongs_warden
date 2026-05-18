package com.mopl.mopl.domain.watchingSession.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ContentChatSendRequest(
        @NotBlank(message = "메시지 내용을 입력해주세요.")
        String content
) {
}
