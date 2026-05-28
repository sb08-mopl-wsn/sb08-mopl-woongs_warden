package com.mopl.mopl.infrastructure.kafka.event;

import java.util.UUID;

public record ContentDeleteEvent
(
        UUID contentId,
        String thumbnailKey
) {}
