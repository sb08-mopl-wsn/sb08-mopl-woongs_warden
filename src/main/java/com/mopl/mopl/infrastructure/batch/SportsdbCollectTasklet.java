package com.mopl.mopl.infrastructure.batch;

import com.mopl.mopl.domain.content.entity.Content;
import com.mopl.mopl.domain.content.repository.ContentRepository;
import com.mopl.mopl.infrastructure.batch.exception.SportsdbBatchCollectException;
import com.mopl.mopl.infrastructure.external.constants.ExternalApiConstants;
import com.mopl.mopl.infrastructure.external.sportsdb.SportsdbApiClient;
import com.mopl.mopl.infrastructure.external.sportsdb.dto.SportsdbEvent;
import com.mopl.mopl.infrastructure.external.sportsdb.mapper.SportsdbContentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
public class SportsdbCollectTasklet implements Tasklet
{
    private final SportsdbApiClient sportsdbApiClient;
    private final SportsdbContentMapper sportsdbContentMapper;
    private final ContentRepository contentRepository;

    @Override
    public @Nullable RepeatStatus execute(@NonNull StepContribution contribution, @NonNull ChunkContext chunkContext) throws Exception {
        try {
            int saved = 0;
            int failed = 0;

            for (int leagueId: ExternalApiConstants.LEAGUE_IDS) {
                try {
                    List<SportsdbEvent> events = sportsdbApiClient.fetchSeasonEvents(leagueId);

                    for (SportsdbEvent event : events) {
                        Content content = sportsdbContentMapper.sportToContent(event);
                        boolean exists = contentRepository.existsByExternalIdAndContentType(content.getExternalId(), content.getContentType());

                        if (!exists) {
                            contentRepository.save(content);
                            saved++;
                        }
                    }
                } catch (Exception e) {
                    failed++;
                    log.warn("Sportsdb 리그 {} 수집 실패", leagueId);
                }
            }

            log.info("Sportsdb 수집 완료 - 저장 {}건, 실패 {}건", saved, failed);
            return RepeatStatus.FINISHED;
        } catch (Exception e) {
            throw new SportsdbBatchCollectException();
        }

    }
}
