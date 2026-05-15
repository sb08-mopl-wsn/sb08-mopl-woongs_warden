package com.mopl.mopl.infrastructure.batch;

import com.mopl.mopl.domain.content.entity.Content;
import com.mopl.mopl.domain.content.repository.ContentRepository;
import com.mopl.mopl.infrastructure.external.constants.ExternalApiConstants;
import com.mopl.mopl.infrastructure.external.sportsdb.SportsdbApiClient;
import com.mopl.mopl.infrastructure.external.sportsdb.dto.SportsdbEvent;
import com.mopl.mopl.infrastructure.external.sportsdb.mapper.SportsdbContentMapper;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
public class SportsdbCollectTasklet implements Tasklet
{
    private final SportsdbApiClient sportsdbApiClient;
    private final SportsdbContentMapper sportsdbContentMapper;
    private final ContentRepository contentRepository;
    private final EntityManager entityManager;
    private final MeterRegistry meterRegistry;

    @Override
    public @Nullable RepeatStatus execute(@NonNull StepContribution contribution, @NonNull ChunkContext chunkContext) throws Exception {
        int saved = 0;
        int failed = 0;

        for (int leagueId: ExternalApiConstants.LEAGUE_IDS) {
            try {
                List<SportsdbEvent> events = sportsdbApiClient.fetchDayEvents(leagueId);

                for (SportsdbEvent event : events) {
                    Content content = sportsdbContentMapper.sportToContent(event);
                    if (contentRepository.existsByExternalIdAndContentType(
                            content.getExternalId(), content.getContentType())) {
                        meterRegistry.counter("mopl.batch.sportsdb.skipped").increment();
                        continue;
                    }
                    try {
                        contentRepository.saveAndFlush(content);
                        saved++;
                    } catch (DataIntegrityViolationException e) {
                        log.debug("중복 콘텐츠 경합으로 저장 스킵: externalId={}", content.getExternalId());
                        meterRegistry.counter("mopl.batch.sportsdb.skipped").increment();
                        entityManager.clear();
                    }
                }
            } catch (Exception e) {
                failed++;
                log.warn("Sportsdb 리그 {} 수집 실패", leagueId);
            }
        }

        meterRegistry.counter("mopl.batch.sportsdb.saved").increment(saved);
        meterRegistry.counter("mopl.batch.sportsdb.failed").increment(failed);

        log.info("Sportsdb 수집 완료 - 저장 {}건, 실패 {}건", saved, failed);
        return RepeatStatus.FINISHED;
    }
}
