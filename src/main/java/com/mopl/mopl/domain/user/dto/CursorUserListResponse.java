package com.mopl.mopl.domain.user.dto;

import com.mopl.mopl.domain.user.entity.SortBy;
import com.mopl.mopl.domain.user.entity.SortDirection;

import java.util.List;
import java.util.UUID;

public record CursorUserListResponse(
        List<UserDto> data,
        String nextCursor,          // 다음 페이지 조회를 위한 커서
        UUID nextIdAfter,           // 보조 커서 (동일 시간대 생성 알림 정렬용 ID)
        Boolean hasNext,            // 다음 페이지 존재 여부
        long totalCount,            // 사용자수
        SortBy sortBy,              // 정렬 기준(createdAt)
        SortDirection sortDirection        // 정렬 방향
) {
}