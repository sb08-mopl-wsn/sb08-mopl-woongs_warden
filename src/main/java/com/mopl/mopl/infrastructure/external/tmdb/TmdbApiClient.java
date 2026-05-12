package com.mopl.mopl.infrastructure.external.tmdb;

import com.mopl.mopl.infrastructure.external.constants.ExternalApiConstants;
import com.mopl.mopl.infrastructure.external.exception.ApiEmptyResponseException;
import com.mopl.mopl.infrastructure.external.tmdb.dto.response.TmdbGenreListResponse;
import com.mopl.mopl.infrastructure.external.tmdb.dto.response.TmdbMovieListResponse;
import com.mopl.mopl.infrastructure.external.tmdb.dto.response.TmdbTvListResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.stream.Collectors;

@Component
public class TmdbApiClient
{
    private final RestClient restClient;

    public TmdbApiClient(@Qualifier("tmdbRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * TMDB API를 통해 영화 목록을 조회한다.
     *
     * @param page 조회할 페이지 번호
     * @return 영화 목록 및 페이지 정보
     */
    public TmdbMovieListResponse discoverMovies(int page) {
        TmdbMovieListResponse response = restClient.get()
                .uri(ExternalApiConstants.DISCOVER_MOVIE, page)
                .retrieve()
                .body(TmdbMovieListResponse.class);

        if (response == null) {
            throw new ApiEmptyResponseException();
        }

        return response;
    }

    /**
     * TMDB API를 통해 TV 프로그램 목록을 조회한다.
     *
     * @param page 조회할 페이지 번호
     * @return TV 프로그램 목록 및 페이지 정보
     */
    public TmdbTvListResponse discoverTv(int page) {
        TmdbTvListResponse response = restClient.get()
                .uri(ExternalApiConstants.DISCOVER_TV, page)
                .retrieve()
                .body(TmdbTvListResponse.class);

        if (response == null) {
            throw new ApiEmptyResponseException();
        }

        return response;
    }

    /**
     * TMDB API를 통해 장르 목록을 조회하고, 장르 ID와 이름을 매핑한 Map을 반환한다.
     *
     * @param uri 장르 목록 조회에 사용할 API URI
     * @return [장르 ID, 장르 이름]이 담긴 Map
     */
    public Map<Integer, String> fetchGenreMap(String uri) {
        TmdbGenreListResponse response = restClient.get()
                .uri(uri)
                .retrieve()
                .body(TmdbGenreListResponse.class);

        if (response != null && response.genres() != null) {
            return response.genres().stream()
                    .collect(Collectors.toMap(
                            TmdbGenreListResponse.Genre::id,
                            TmdbGenreListResponse.Genre::name,
                            (existing, replace) -> existing
                    ));
        }
        return Map.of();
    }
}
