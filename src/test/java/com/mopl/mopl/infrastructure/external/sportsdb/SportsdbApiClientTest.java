package com.mopl.mopl.infrastructure.external.sportsdb;

import com.mopl.mopl.infrastructure.external.sportsdb.dto.SportsdbEvent;
import com.mopl.mopl.infrastructure.external.sportsdb.dto.response.SportsdbEventResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.RequestHeadersSpec;
import org.springframework.web.client.RestClient.RequestHeadersUriSpec;
import org.springframework.web.client.RestClient.ResponseSpec;

import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"rawtypes", "unchecked"})
@DisplayName("SportsdbClient Unit Test")
class SportsdbApiClientTest
{
    @Mock private RestClient restClient;
    @Mock private RequestHeadersUriSpec requestHeadersUriSpec;
    @Mock private RequestHeadersSpec requestHeadersSpec;
    @Mock private ResponseSpec responseSpec;

    private SportsdbApiClient sportsDbApiClient;

    @BeforeEach
    void setUp() {
        sportsDbApiClient = new SportsdbApiClient(restClient);
    }

    @Test
    @DisplayName("시즌 경기 성공")
    void givenValidLeagueId_whenFetchSeasonEvents_thenReturnsEventList() {
        // given
        SportsdbEvent event = new SportsdbEvent(
                "Liverpool vs Chelsea", "1", "https://thumb.jpg",
                "English Premier League 2025-08-15 Liverpool vs Chelsea", "Anfield",
                "English Premier League", "Soccer", "123", "2025-08-15"
        );
        SportsdbEventResponse expected = new SportsdbEventResponse(List.of(event));

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), anyString(), anyInt())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(SportsdbEventResponse.class)).thenReturn(expected);

        // when
        List<SportsdbEvent> result = sportsDbApiClient.fetchDayEvents(4328);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().strEvent()).isEqualTo("Liverpool vs Chelsea");
    }

    @Test
    @DisplayName("응답이 null이면 빈 리스트 반환")
    void givenNullResponse_whenFetchSeasonEvents_thenReturnsEmptyList() {
        // given
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), anyString(), anyInt())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(SportsdbEventResponse.class)).thenReturn(null);

        // when
        List<SportsdbEvent> result = sportsDbApiClient.fetchDayEvents(4328);

        // then
        assertThat(result).isEmpty();
    }
}