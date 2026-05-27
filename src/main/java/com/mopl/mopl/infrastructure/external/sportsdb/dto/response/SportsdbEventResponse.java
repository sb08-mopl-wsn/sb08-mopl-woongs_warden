package com.mopl.mopl.infrastructure.external.sportsdb.dto.response;

import com.mopl.mopl.infrastructure.external.sportsdb.dto.SportsdbEvent;

import java.util.List;

public record SportsdbEventResponse
(
        List<SportsdbEvent> events
) {}
