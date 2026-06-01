package com.mopl.mopl.global.redis.dto;

import java.util.UUID;

public record WebsocketPayload<T> (
        UUID contentId,
        String targetType,
        T data
) {
}
