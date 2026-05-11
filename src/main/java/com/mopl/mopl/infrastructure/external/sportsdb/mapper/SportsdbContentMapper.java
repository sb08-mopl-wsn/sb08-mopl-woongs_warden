package com.mopl.mopl.infrastructure.external.sportsdb.mapper;

import com.mopl.mopl.domain.content.entity.Content;
import com.mopl.mopl.domain.content.entity.ContentType;
import com.mopl.mopl.infrastructure.external.sportsdb.SportsdbApiClient;
import com.mopl.mopl.infrastructure.external.sportsdb.dto.SportsdbEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@RequiredArgsConstructor
@Component
public class SportsdbContentMapper
{
    private final SportsdbApiClient sportsdbApiClient;

    public Content sportToContent(SportsdbEvent sportDbEvent) {
        return Content.builder()
                .title(sportDbEvent.strEvent())
                .description(sportDbEvent.strFilename())
                .contentType(ContentType.sport)
                .thumbnailKey(sportDbEvent.strThumb())
                .releaseDate(parseDate(sportDbEvent.dateEvent()))
                .tags(List.of(sportDbEvent.strSport(), sportDbEvent.strLeague()))
                .externalId(sportDbEvent.idEvent())
                .build();
    }

    private Instant parseDate(String date) {
        if (date == null || date.isBlank()) return null;
        return LocalDate.parse(date)
                .atStartOfDay()
                .toInstant(ZoneOffset.UTC);
    }
}
