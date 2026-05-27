package com.mopl.mopl.infrastructure.elasticsearch.event;

import com.mopl.mopl.domain.content.entity.Content;

public record ContentIndexEvent
(
        Content content
) {}
