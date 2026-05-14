package com.mopl.mopl.global.event.user;

import java.util.UUID;

public record UserUpdateProfileEvent(
        UUID userId
) {
}
