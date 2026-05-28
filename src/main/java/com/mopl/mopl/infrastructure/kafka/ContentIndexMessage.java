package com.mopl.mopl.infrastructure.kafka;

import java.util.UUID;

public record ContentIndexMessage
(
        UUID contentId,
        ActionType action
)
{
    public enum ActionType {
        INDEX,
        DELETE
    }
}
