package com.mopl.mopl.global.event.user;

import java.util.UUID;

public record UserEvent(
        UUID userId,
        String name
) {
}
