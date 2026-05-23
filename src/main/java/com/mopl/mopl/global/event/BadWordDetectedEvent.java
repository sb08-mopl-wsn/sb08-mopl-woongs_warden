package com.mopl.mopl.global.event;

import java.util.UUID;

public record BadWordDetectedEvent(
        UUID userId,
        String content
) {
}
