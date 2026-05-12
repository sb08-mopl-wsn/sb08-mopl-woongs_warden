package com.mopl.mopl.domain.watchingSession.dto.response;

import com.mopl.mopl.domain.content.dto.response.ContentSummary;
import com.mopl.mopl.domain.content.entity.ContentType;
import com.mopl.mopl.domain.user.dto.UserSummary;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WatchingSessionDto(
        // 시청 세션 ID
        UUID id,
        // 시청 세션 생성 시간
        Instant createdAt,
        // 시청자 정보
        UserSummary watcher,
        // 시청 중인 컨텐츠 정보
        ContentSummary content
) {
}
