package com.mopl.mopl.infrastructure.external.sportsdb.dto;

public record SportsdbEvent
(
        String strEvent,
        String intRound,
        String strThumb,
        String strFilename,
        String strVenue,
        String strLeague,
        String strSport,
        String idEvent,
        String dateEvent
) {}
