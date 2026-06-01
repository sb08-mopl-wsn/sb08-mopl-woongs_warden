package com.mopl.mopl.infrastructure.kafka.event;

import com.mopl.mopl.domain.content.entity.Content;

public record ContentIndexEvent
(
        Content content
) {}
