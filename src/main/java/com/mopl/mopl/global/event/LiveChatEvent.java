package com.mopl.mopl.global.event;

import com.mopl.mopl.domain.watchingSession.dto.response.ContentChatDto;

import java.util.UUID;

public record LiveChatEvent(
        UUID contentId,
        ContentChatDto chatDto
) {
}
