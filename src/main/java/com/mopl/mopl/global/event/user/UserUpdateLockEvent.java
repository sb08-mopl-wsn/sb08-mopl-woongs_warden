package com.mopl.mopl.global.event.user;

import java.util.UUID;

public record UserUpdateLockEvent(
        UUID userId,
        String name,
        boolean isLocked,
        String userEamil
) {
}