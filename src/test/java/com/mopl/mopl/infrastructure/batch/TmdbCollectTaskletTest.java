package com.mopl.mopl.infrastructure.batch;

import com.mopl.mopl.domain.content.entity.Content;
import com.mopl.mopl.domain.content.entity.ContentType;
import com.mopl.mopl.domain.content.repository.ContentRepository;
import com.mopl.mopl.infrastructure.external.tmdb.TmdbApiClient;
import com.mopl.mopl.infrastructure.external.tmdb.dto.TmdbMovie;
import com.mopl.mopl.infrastructure.external.tmdb.dto.TmdbTv;
import com.mopl.mopl.infrastructure.external.tmdb.dto.response.TmdbMovieListResponse;
import com.mopl.mopl.infrastructure.external.tmdb.dto.response.TmdbTvListResponse;
import com.mopl.mopl.infrastructure.external.tmdb.mapper.TmdbContentMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TmdbCollectTasklet Test")
class TmdbCollectTaskletTest
{
    @Mock private TmdbApiClient tmdbApiClient;
    @Mock private TmdbContentMapper tmdbContentMapper;
    @Mock private ContentRepository contentRepository;
    @Mock private StepContribution contribution;
    @Mock private ChunkContext chunkContext;

    private TmdbCollectTasklet tasklet;

    @BeforeEach
    void setUp() {
        tasklet = new TmdbCollectTasklet(tmdbApiClient, tmdbContentMapper, contentRepository, 1);
    }

    @Test
    @DisplayName("TMDB 콘텐츠 수집 성공")
    void givenNewContents_whenExecute_thenSavesAll() throws Exception {
        // given
        TmdbMovie movie = new TmdbMovie(1, "영화", "설명", "/poster.jpg", "2025-01-01", List.of(28));
        TmdbTv tv = new TmdbTv(2, "TV", "설명", "/poster.jpg", "2025-01-01", List.of(18));

        when(tmdbApiClient.discoverMovies(1)).thenReturn(new TmdbMovieListResponse(1, List.of(movie), 1, 1));
        when(tmdbApiClient.discoverTv(1)).thenReturn(new TmdbTvListResponse(1, List.of(tv), 1, 1));

        Content movieContent = Content.builder().title("영화").externalId("1").contentType(ContentType.movie).build();
        Content tvContent = Content.builder().title("TV").externalId("2").contentType(ContentType.tvSeries).build();

        when(tmdbContentMapper.movieToContent(movie)).thenReturn(movieContent);
        when(tmdbContentMapper.tvToContent(tv)).thenReturn(tvContent);

        // when
        RepeatStatus status = tasklet.execute(contribution, chunkContext);

        // then
        assertThat(status).isEqualTo(RepeatStatus.FINISHED);
        verify(contentRepository, times(2)).save(any(Content.class));
    }

    @Test
    @DisplayName("중복 데이터는 저장하지 않음")
    void givenExistingContent_whenExecute_thenSkipsDuplicate() throws Exception {
        // given
        TmdbMovie movie = new TmdbMovie(1, "영화", "설명", "/poster.jpg", "2025-01-01", List.of(28));

        when(tmdbApiClient.discoverMovies(1)).thenReturn(new TmdbMovieListResponse(1, List.of(movie), 1, 1));
        when(tmdbApiClient.discoverTv(1)).thenReturn(new TmdbTvListResponse(1, List.of(), 1, 1));

        Content movieContent = Content.builder().title("영화").externalId("1").contentType(ContentType.movie).build();

        when(tmdbContentMapper.movieToContent(movie)).thenReturn(movieContent);
        when(contentRepository.save(any(Content.class)))
                .thenThrow(new DataIntegrityViolationException("중복"));

        // when
        RepeatStatus status = tasklet.execute(contribution, chunkContext);

        // then
        assertThat(status).isEqualTo(RepeatStatus.FINISHED);
    }

    @Test
    @DisplayName("API 실패해도 나머지 계속 수집")
    void givenMovieApiFails_whenExecute_thenContinuesTvCollection() throws Exception {
        // given
        when(tmdbApiClient.discoverMovies(1)).thenThrow(new RuntimeException("API 오류"));

        TmdbTv tv = new TmdbTv(2, "TV", "설명", "/poster.jpg", "2025-01-01", List.of(18));
        when(tmdbApiClient.discoverTv(1)).thenReturn(new TmdbTvListResponse(1, List.of(tv), 1, 1));

        Content tvContent = Content.builder().title("TV").externalId("2").contentType(ContentType.tvSeries).build();
        when(tmdbContentMapper.tvToContent(tv)).thenReturn(tvContent);

        // when
        RepeatStatus status = tasklet.execute(contribution, chunkContext);

        // then
        assertThat(status).isEqualTo(RepeatStatus.FINISHED);
        verify(contentRepository, times(1)).save(any(Content.class));
    }
}