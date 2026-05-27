package com.mopl.mopl.domain.dm.dto;

import com.mopl.mopl.domain.user.dto.UserSummary;
import java.time.Instant;
import java.util.UUID;

public record DirectMessageDto(
    UUID id,
    UUID conversationId,
    String content,
    UserSummary sender,
    UserSummary receiver,
    Instant createdAt
) {

}
