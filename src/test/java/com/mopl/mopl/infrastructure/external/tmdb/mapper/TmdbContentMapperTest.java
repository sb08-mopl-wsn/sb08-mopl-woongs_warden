package com.mopl.mopl.infrastructure.external.tmdb.mapper;

import com.mopl.mopl.domain.content.entity.Content;
import com.mopl.mopl.domain.content.entity.ContentType;
import com.mopl.mopl.infrastructure.external.constants.ExternalApiConstants;
import com.mopl.mopl.infrastructure.external.tmdb.TmdbApiClient;
import com.mopl.mopl.infrastructure.external.tmdb.dto.TmdbMovie;
import com.mopl.mopl.infrastructure.external.tmdb.dto.TmdbTv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TmdbContentMapper Test")
class TmdbContentMapperTest
{
    @Mock private TmdbApiClient apiClient;
    private TmdbContentMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new TmdbContentMapper(apiClient);
    }

    @Test
    @DisplayName("영화 변환 성공")
    void givenTmdbMovie_whenMovieToContent_thenReturnsContent() {
        when(apiClient.fetchGenreMap(ExternalApiConstants.MOVIE_GENRE_LIST))
                .thenReturn(Map.of(28, "액션", 53, "스릴러"));

        TmdbMovie movie = new TmdbMovie(1, "더 러닝 맨", "액션 블록버스터",
                "/poster.jpg", "2025-11-07", List.of(28, 53));

        Content content = mapper.movieToContent(movie);

        assertThat(content.getTitle()).isEqualTo("더 러닝 맨");
        assertThat(content.getDescription()).isEqualTo("액션 블록버스터");
        assertThat(content.getContentType()).isEqualTo(ContentType.movie);
        assertThat(content.getThumbnailKey()).isEqualTo(ExternalApiConstants.IMAGE_BASE_URL + "/poster.jpg");
        assertThat(content.getExternalId()).isEqualTo("1");
    }

    @Test
    @DisplayName("TV 변환 성공")
    void givenTmdbTv_whenTvToContent_thenReturnsContent() {
        when(apiClient.fetchGenreMap(ExternalApiConstants.TV_GENRE_LIST))
                .thenReturn(Map.of(18, "드라마", 10765, "SF & 판타지"));

        TmdbTv tv = new TmdbTv(2, "기묘한 이야기", "SF 드라마",
                "/tv_poster.jpg", "2025-06-01", List.of(18, 10765));

        Content content = mapper.tvToContent(tv);

        assertThat(content.getTitle()).isEqualTo("기묘한 이야기");
        assertThat(content.getContentType()).isEqualTo(ContentType.tvSeries);
    }

    @Test
    @DisplayName("posterPath가 null이면 thumbnailKey도 null")
    void givenNullPosterPath_whenMovieToContent_thenThumbnailKeyIsNull() {
        TmdbMovie movie = new TmdbMovie(3, "테스트", "설명",
                null, "2025-01-01", List.of(28));

        Content content = mapper.movieToContent(movie);

        assertThat(content.getThumbnailKey()).isNull();
    }

    @Test
    @DisplayName("releaseDate가 빈값이면 null")
    void givenEmptyReleaseDate_whenMovieToContent_thenReleaseDateIsNull() {
        TmdbMovie movie = new TmdbMovie(4, "테스트", "설명",
                "/poster.jpg", "", List.of(28));

        Content content = mapper.movieToContent(movie);

        assertThat(content.getReleaseDate()).isNull();
    }
}