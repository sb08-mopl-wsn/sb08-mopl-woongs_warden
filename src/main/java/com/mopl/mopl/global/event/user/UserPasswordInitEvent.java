package com.mopl.mopl.global.event.user;

import java.time.Instant;
import java.util.UUID;

public record UserPasswordInitEvent(
        String username,
        UUID userId,
        String email,
        String password,
        Instant expiredAt
) {
}