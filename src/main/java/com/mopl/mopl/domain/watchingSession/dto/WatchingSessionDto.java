package com.mopl.mopl.domain.watchingSession.dto;

import com.mopl.mopl.domain.content.entity.ContentType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

// 전용 DTO 객체를 내부 레코드로 만들어서 필요한 정보만 제공
public record WatchingSessionDto(
        // 시청 세션 ID
        UUID id,
        // 시청 세션 생성 시간
        Instant createdAt,
        // 시청자 정보
        WatcherDto watcher,
        // 시청 중인 컨텐츠 정보
        ContentInfoDto content
) {
    public record WatcherDto(
            UUID userId,
            String name,
            String profileImageUrl
    ) {
    }

    public record ContentInfoDto(
            UUID id,
            ContentType type,
            String title,
            String description,
            String thumbnailUrl,
            List<String> tags,
            BigDecimal averageRating,
            Integer reviewCount
    ) {
    }
}
