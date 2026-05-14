package com.mopl.mopl.global.event;

import com.mopl.mopl.domain.dm.dto.DirectMessageDto;

import java.util.UUID;

public record DirectMessageSentEvent(
        UUID conversationId,
        DirectMessageDto messageDto
) {
}
