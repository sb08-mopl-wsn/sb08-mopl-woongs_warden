package com.mopl.mopl.global.event.dto;

import java.util.UUID;

public record WebsocketPayload<T> (
        UUID contentId,
        String targetType,
        T data
) {
}
