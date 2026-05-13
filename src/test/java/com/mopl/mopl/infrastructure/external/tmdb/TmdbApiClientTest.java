package com.mopl.mopl.infrastructure.external.tmdb;

import com.mopl.mopl.infrastructure.external.constants.ExternalApiConstants;
import com.mopl.mopl.infrastructure.external.tmdb.dto.TmdbMovie;
import com.mopl.mopl.infrastructure.external.tmdb.dto.TmdbTv;
import com.mopl.mopl.infrastructure.external.tmdb.dto.response.TmdbGenreListResponse;
import com.mopl.mopl.infrastructure.external.tmdb.dto.response.TmdbGenreListResponse.Genre;
import com.mopl.mopl.infrastructure.external.tmdb.dto.response.TmdbMovieListResponse;
import com.mopl.mopl.infrastructure.external.tmdb.dto.response.TmdbTvListResponse;
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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"rawtypes", "unchecked"})
@DisplayName("TmdbApiClient Unit Test")
class TmdbApiClientTest
{
    @Mock private RestClient restClient;
    @Mock private RequestHeadersSpec requestHeadersSpec;
    @Mock private RequestHeadersUriSpec requestHeadersUriSpec;
    @Mock private ResponseSpec responseSpec;

    private TmdbApiClient tmdbApiClient;

    @BeforeEach
    void setUp() {
        tmdbApiClient = new TmdbApiClient(restClient);
    }

    @Test
    @DisplayName("영화 목록 조회 성공")
    void givenValidPage_whenDiscoverMovies_thenReturnsMovieList() {
        // given
        TmdbMovie movie = new TmdbMovie(1, "테스트 영화", "설명", "/poster.jpg", "2025-01-01", List.of(28));
        TmdbMovieListResponse expected = new TmdbMovieListResponse(1, List.of(movie), 1, 1);

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), anyInt())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(TmdbMovieListResponse.class)).thenReturn(expected);

        // when
        TmdbMovieListResponse result = tmdbApiClient.discoverMovies(1);

        // then
        assertThat(result.results()).hasSize(1);
        assertThat(result.results().getFirst().title()).isEqualTo("테스트 영화");
    }

    @Test
    @DisplayName("TV 목록 조회 성공")
    void givenValidPage_whenDiscoverTv_thenReturnsTvList() {
        // given
        TmdbTv tv = new TmdbTv(1, "테스트 TV", "설명", "/poster.jpg", "2025-01-01", List.of(18));
        TmdbTvListResponse expected = new TmdbTvListResponse(1, List.of(tv), 1, 1);

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), anyInt())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(TmdbTvListResponse.class)).thenReturn(expected);

        // when
        TmdbTvListResponse result = tmdbApiClient.discoverTv(1);

        // then
        assertThat(result.results()).hasSize(1);
        assertThat(result.results().getFirst().name()).isEqualTo("테스트 TV");
    }

    @Test
    @DisplayName("장르 맵 조회 성공")
    void givenValidUri_whenFetchGenreMap_thenReturnsGenreMap() {
        // given
        Genre action = new Genre(28, "액션");
        Genre thriller = new Genre(53, "스릴러");
        TmdbGenreListResponse expected = new TmdbGenreListResponse(List.of(action, thriller));

        doReturn(requestHeadersUriSpec).when(restClient).get();
        doReturn(requestHeadersSpec).when(requestHeadersUriSpec).uri(anyString());
        doReturn(responseSpec).when(requestHeadersSpec).retrieve();
        doReturn(expected).when(responseSpec).body(TmdbGenreListResponse.class);

        // when
        Map<Integer, String> result = tmdbApiClient.fetchGenreMap(ExternalApiConstants.MOVIE_GENRE_LIST);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(28)).isEqualTo("액션");
        assertThat(result.get(53)).isEqualTo("스릴러");
    }

    @Test
    @DisplayName("장르 응답이 null이면 빈 맵 반환")
    void givenNullResponse_whenFetchGenreMap_thenReturnsEmptyMap() {
        // given
        doReturn(requestHeadersUriSpec).when(restClient).get();
        doReturn(requestHeadersSpec).when(requestHeadersUriSpec).uri(anyString());
        doReturn(responseSpec).when(requestHeadersSpec).retrieve();
        doReturn(null).when(responseSpec).body(TmdbGenreListResponse.class);

        // when
        Map<Integer, String> result = tmdbApiClient.fetchGenreMap(ExternalApiConstants.MOVIE_GENRE_LIST);

        // then
        assertThat(result).isEmpty();
    }
}