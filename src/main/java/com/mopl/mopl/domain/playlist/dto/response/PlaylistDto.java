package com.mopl.mopl.domain.playlist.dto.response;

import com.mopl.mopl.domain.user.dto.UserSummary;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PlaylistDto(
    UUID id,
    UserSummary owner,
    String title,
    String description,
    Instant updatedAt,
    Long subscriberCount,
    Boolean subscribedByMe,

    // TODO: ContentSummary DTO 생성 후 아래 라인의 주석을 해제, List<Object> 라인을 제거
    // List<ContentSummary> contents
    List<Object> contents // 임시 List<Object>사용
) {
}