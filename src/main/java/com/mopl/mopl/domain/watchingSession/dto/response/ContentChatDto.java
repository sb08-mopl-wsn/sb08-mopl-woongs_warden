package com.mopl.mopl.domain.watchingSession.dto.response;

import com.mopl.mopl.domain.user.dto.UserSummary;

public record ContentChatDto(
        UserSummary sender,
        String content
) {
}
