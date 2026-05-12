package com.mopl.mopl.global.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public record CursorPaginationRequest(
    String cursor,
    UUID idAfter,
    @Positive(message = "limit는 1 이상이어야 합니다.")
    @Max(value = 100, message = "limit은 100 이하의 값이어야 합니다.")
    Integer limit,
    String sortDirection,
    String sortBy
) {

  // 기본값 설정 컴팩트 생성자
  public CursorPaginationRequest {
    if (limit == null) limit = 20;
    if (sortDirection == null) sortDirection = "DESCENDING";
    if (sortBy == null) sortBy = "createdAt";
  }
}
