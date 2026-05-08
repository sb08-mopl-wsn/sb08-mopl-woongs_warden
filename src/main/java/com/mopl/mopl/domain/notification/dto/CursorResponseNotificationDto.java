package com.mopl.mopl.domain.notification.dto;

import java.util.List;
import java.util.UUID;

public record CursorResponseNotificationDto(
    List<NotificationDto> data, // 알림 데이터 목록
    String nextCursor,          // 다음 페이지 조회를 위한 커서
    UUID nextIdAfter,           // 보조 커서 (동일 시간대 생성 알림 정렬용 ID)
    boolean hasNext,            // 다음 페이지 존재 여부
    long totalCount,            // 사용자의 총 알림 개수
    String sortBy,              // 정렬 기준(createdAt)
    String sortDirection        // 정렬 방향
) {

}
