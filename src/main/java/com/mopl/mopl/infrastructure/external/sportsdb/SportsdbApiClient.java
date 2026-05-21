package com.mopl.mopl.infrastructure.external.sportsdb;

import com.mopl.mopl.infrastructure.external.constants.ExternalApiConstants;
import com.mopl.mopl.infrastructure.external.exception.ApiEmptyResponseException;
import com.mopl.mopl.infrastructure.external.sportsdb.dto.SportsdbEvent;
import com.mopl.mopl.infrastructure.external.sportsdb.dto.response.SportsdbEventResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.util.List;

@Component
public class SportsdbApiClient
{
    private final RestClient restClient;

    public SportsdbApiClient(@Qualifier("sportsdbRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * SportsDB API를 통해 특정 리그의 시즌별 경기 일정 및 결과를 조회한다.
     *
     * @param leagueId  조회할 리그의 고유 ID
     * @return 해당 리그 및 시즌의 대한 정보 목록
     */
    public List<SportsdbEvent> fetchDayEvents(int leagueId) {
        String day = LocalDate.now().plusDays(3).toString();

        SportsdbEventResponse response;
        try {
            response = restClient.get()
                    .uri(ExternalApiConstants.EVENTS_DAY_PATH, day, leagueId)
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
