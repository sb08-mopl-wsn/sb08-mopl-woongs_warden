package com.mopl.mopl.infrastructure.batch;

import com.mopl.mopl.domain.content.entity.Content;
import com.mopl.mopl.domain.content.repository.ContentRepository;
import com.mopl.mopl.infrastructure.batch.exception.TmdbBatchCollectException;
import com.mopl.mopl.infrastructure.external.tmdb.TmdbApiClient;
import com.mopl.mopl.infrastructure.external.tmdb.mapper.TmdbContentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import java.util.List;
import java.util.function.Function;

@RequiredArgsConstructor
@Slf4j
public class TmdbCollectTasklet implements Tasklet
{
    private final TmdbApiClient tmdbApiClient;
    private final TmdbContentMapper tmdbContentMapper;
    private final ContentRepository contentRepository;
    private final int totalPages;

    @Override
    public @Nullable RepeatStatus execute(@NonNull StepContribution contribution, @NonNull ChunkContext chunkContext) throws Exception {
        try {
            int failed = 0;
            int saved = 0;

            for (int page = 1; page <= totalPages; page++) {
                try {
                    saved += importMovies(page);
                } catch (Exception e) {
                    failed++;
                    log.warn("TMDB 영화 {}페이지 수집 실패", page, e);
                }

                try {
                    saved += importTv(page);
                } catch (Exception e) {
                    failed++;
                    log.warn("TMDB TV {}페이지 수집 실패", page, e);
                }
            }

            log.info("TMDB 수집 완료 - 저장: {}건, 실패: {}건", saved, failed);
            return RepeatStatus.FINISHED;
        } catch (Exception e) {
            throw new TmdbBatchCollectException();
        }
    }

    private int importMovies(int page) {
        return importContents(
                tmdbApiClient.discoverMovies(page).results(),
                tmdbContentMapper::movieToContent
        );
    }

    private int importTv(int page) {
        return importContents(
                tmdbApiClient.discoverTv(page).results(),
                tmdbContentMapper::tvToContent
        );
    }

    private <T> int importContents(List<T> items, Function<T, Content> mapper) {
        int saved = 0;
        for (T item : items) {
            Content content = mapper.apply(item);
            boolean exists = contentRepository.existsByExternalIdAndContentType(
                    content.getExternalId(), content.getContentType()
            );
            if (!exists) {
                contentRepository.save(content);
                saved++;
            }
        }
        return saved;
    }
}
