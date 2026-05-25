package com.mopl.mopl.global.event.kafka;

import java.time.Instant;
import java.util.UUID;

public record UserSecurityEventMessage(
    String eventType,
    UUID userId,
    String email,
    String name,
    String detail,
    Instant occurredAt
) {
}
