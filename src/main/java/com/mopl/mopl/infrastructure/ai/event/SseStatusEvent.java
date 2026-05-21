package com.mopl.mopl.infrastructure.ai.event;

public record SseStatusEvent
(
        String stage,
        String message
) {}
