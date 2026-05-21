package com.mopl.mopl.infrastructure.ai.event;

public record SseErrorEvent
(
        String code,
        String message
) {}
