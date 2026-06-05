package com.mopl.mopl.domain.review.event;

import java.util.UUID;

public record ReviewChangedEvent
(
        UUID userId
) {}
