package com.mopl.mopl.infrastructure.external.sportsdb;

import com.mopl.mopl.infrastructure.external.constants.ExternalApiConstants;
import com.mopl.mopl.infrastructure.external.exception.ApiEmptyResponseException;
import com.mopl.mopl.infrastructure.external.sportsdb.dto.SportsdbEvent;
import com.mopl.mopl.infrastructure.external.sportsdb.dto.response.SportsdbEventResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class SportsdbApiClient
{
    private final RestClient restClient;
    private final Clock clock;

    public SportsdbApiClient(@Qualifier("sportsdbRestClient") RestClient restClient, Clock clock) {
        this.restClient = restClient;
        this.clock = clock;
    }

    /**
     * 현재 날짜 기준 3일 후의 예정 경기 목록을 조회한다.
     * <p>
     * 타임존 의존성을 제거하기 위해 {@link Clock}을 주입받아 날짜를 계산한다.
     *
     * @param leagueId 조회할 리그 ID
     * @return 3일 후 예정 경기 목록
     */
    public List<SportsdbEvent> fetchDayEvents(int leagueId) {
        String date = LocalDate.now(clock).plusDays(3).format(DateTimeFormatter.ISO_LOCAL_DATE);

        SportsdbEventResponse response;
        try {
            response = restClient.get()
                    .uri(ExternalApiConstants.EVENTS_DAY_PATH, date, leagueId)
                    .retrieve()
                    .body(SportsdbEventResponse.class);
        } catch (RestClientException ex) {
            throw new ApiEmptyResponseException();
        }

        return response != null && response.events() != null
                ? response.events()
                : List.of();
    }
}
