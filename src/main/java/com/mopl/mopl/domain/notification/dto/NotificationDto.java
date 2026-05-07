package com.mopl.mopl.domain.notification.dto;

import com.mopl.mopl.domain.notification.entity.NotificationLevel;
import java.time.Instant;
import java.util.UUID;

public record NotificationDto(
    UUID id,
    String title,
    String content,
    NotificationLevel level,
    UUID receiverId,
    Instant createdAt
) {

}
