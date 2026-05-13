package com.mopl.mopl.domain.conversation.dto.response;

import java.util.List;
import java.util.UUID;

public record CursorResponseConversationDto(
    List<ConversationDto> data,
    String nextCursor,
    UUID nextIdAfter,
    boolean hasNext,
    long totalCount,
    String sortBy,
    String sortDirection
) {

}
