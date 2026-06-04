package com.mopl.mopl.domain.conversation.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public record CursorConversationRequest(
    String keywordLike,
    String cursor,
    UUID idAfter,
    @Positive(message = "limit는 1 이상이어야 합니다.")
    @Max(value = 100, message = "limit은 100 이하의 값이어야 합니다.")
    Integer limit,
    String sortDirection,
    String sortBy
) {

  public CursorConversationRequest {
    if (keywordLike != null) {
      keywordLike = keywordLike.trim();
      if (keywordLike.isEmpty()) keywordLike = null;
    }

    if (sortBy != null) {
      sortBy = sortBy.trim();
      if (sortBy.isEmpty()) sortBy = null;
    }

    if (sortDirection != null) {
      sortDirection = sortDirection.trim();
      if (sortDirection.isEmpty()) sortDirection = null;
    }

    if (limit == null) limit = 20;
    if (sortDirection == null || sortDirection.isBlank()) sortDirection = "DESCENDING";
  }
}
