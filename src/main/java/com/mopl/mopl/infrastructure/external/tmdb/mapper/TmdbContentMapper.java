package com.mopl.mopl.infrastructure.external.tmdb.mapper;

import com.mopl.mopl.domain.content.entity.Content;
import com.mopl.mopl.domain.content.entity.ContentType;
import com.mopl.mopl.infrastructure.external.constants.ExternalApiConstants;
import com.mopl.mopl.infrastructure.external.tmdb.TmdbApiClient;
import com.mopl.mopl.infrastructure.external.tmdb.dto.TmdbMovie;
import com.mopl.mopl.infrastructure.external.tmdb.dto.TmdbTv;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class TmdbContentMapper
{
    private final TmdbApiClient apiClient;

    private Map<Integer, String> movieGenreMap;
    private Map<Integer, String> tvGenreMap;

    public TmdbContentMapper(TmdbApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public Content movieToContent(TmdbMovie movie) {
        List<String> tags = resolveGenres(movie.genreIds(), getMovieGenreMap());

        return Content.builder()
                .title(movie.title())
                .description(movie.overview())
                .contentType(ContentType.movie)
                .thumbnailKey(toImageUrl(movie.posterPath()))
                .tags(tags)
                .releaseDate(parseDate(movie.releaseDate()))
                .externalId(String.valueOf(movie.id()))
                .build();
    }

    public Content tvToContent(TmdbTv tv) {
        List<String> tags = resolveGenres(tv.genreIds(), getTvGenreMap());

        return Content.builder()
                .title(tv.name())
                .description(tv.overview())
                .contentType(ContentType.tvSeries)
                .thumbnailKey(toImageUrl(tv.posterPath()))
                .tags(tags)
                .releaseDate(parseDate(tv.firstAirDate()))
                .externalId(String.valueOf(tv.id()))
                .build();
    }

    private Map<Integer, String> getMovieGenreMap() {
        if (movieGenreMap == null) {
            movieGenreMap = apiClient.fetchGenreMap(ExternalApiConstants.MOVIE_GENRE_LIST);
        }
        return movieGenreMap;
    }

    private Map<Integer, String> getTvGenreMap() {
        if (tvGenreMap == null) {
            tvGenreMap = apiClient.fetchGenreMap(ExternalApiConstants.TV_GENRE_LIST);
        }
        return tvGenreMap;
    }

    private String toImageUrl(String posterPath) {
        return posterPath != null ? ExternalApiConstants.IMAGE_BASE_URL + posterPath : null;
    }

    private Instant parseDate(String date) {
        if (date == null || date.isBlank()) return null;
        return LocalDate.parse(date)
                .atStartOfDay()
                .toInstant(ZoneOffset.UTC);
    }

    private List<String> resolveGenres(List<Integer> genreIds, Map<Integer, String> genreMap) {
        return genreIds.stream()
                .map(genreMap::get)
                .filter(Objects::nonNull)
                .toList();
    }
}
