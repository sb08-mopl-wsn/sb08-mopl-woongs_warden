package com.mopl.mopl.infrastructure.config;

import com.mopl.mopl.domain.content.repository.ContentRepository;
import com.mopl.mopl.infrastructure.ai.ContentEmbeddingService;
import com.mopl.mopl.infrastructure.batch.SportsdbCollectTasklet;
import com.mopl.mopl.infrastructure.batch.TmdbCollectTasklet;
import com.mopl.mopl.infrastructure.elasticsearch.ContentIndexService;
import com.mopl.mopl.infrastructure.external.sportsdb.SportsdbApiClient;
import com.mopl.mopl.infrastructure.external.sportsdb.mapper.SportsdbContentMapper;
import com.mopl.mopl.infrastructure.external.tmdb.TmdbApiClient;
import com.mopl.mopl.infrastructure.external.tmdb.mapper.TmdbContentMapper;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class BatchJobConfig
{
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    private final TmdbApiClient tmdbApiClient;
    private final TmdbContentMapper tmdbContentMapper;

    private final SportsdbApiClient sportsdbApiClient;
    private final SportsdbContentMapper sportsdbContentMapper;

    private final ContentRepository contentRepository;
    private final ContentIndexService contentIndexService;
    private final ContentEmbeddingService contentEmbeddingService;

    private final EntityManager entityManager;

    private final MeterRegistry meterRegistry;

    @Value("${external.tmdb.collect-pages:5}")
    private int collectPages;

    /* Job */
    @Bean
    public Job contentCollectJob() {
        return new JobBuilder("contentCollectJob", jobRepository)
                .listener(jobExecutionListener())
                .start(tmdbCollectStep())
                .next(sportsdbCollectStep())
                .preventRestart()
                .build();
    }

    /* Step */
    @Bean
    public Step tmdbCollectStep() {
        return new StepBuilder("tmdbCollectStep", jobRepository)
                .tasklet(tmdbCollectTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Step sportsdbCollectStep() {
        return new StepBuilder("sportsdbCollectStep", jobRepository)
                .tasklet(sportsdbCollectTasklet(), transactionManager)
                .build();
    }

    /* Tasklet */
    @Bean
    public TmdbCollectTasklet tmdbCollectTasklet() {
        return new TmdbCollectTasklet(
                tmdbApiClient, tmdbContentMapper, contentRepository, entityManager, meterRegistry,
                contentIndexService, contentEmbeddingService, collectPages
        );
    }

    @Bean
    public SportsdbCollectTasklet sportsdbCollectTasklet() {
        return new SportsdbCollectTasklet(
                sportsdbApiClient, sportsdbContentMapper, contentRepository, entityManager, meterRegistry,
                contentIndexService, contentEmbeddingService
        );
    }

    @Bean
    public JobExecutionListener jobExecutionListener() {
        return new JobExecutionListener() {
            @Override
            public void beforeJob(@NonNull JobExecution jobExecution) {
                log.debug("Job 시작: {}", jobExecution.getJobInstance().getJobName());
            }

            @Override
            public void afterJob(@NonNull JobExecution jobExecution) {
                LocalDateTime startTime = jobExecution.getStartTime();
                LocalDateTime endTime = jobExecution.getEndTime();
                String jobName = jobExecution.getJobInstance().getJobName();

                if (startTime == null || endTime == null) {
                    log.debug("Job 종료: {} - 상태: {} (시간 정보 없음)",
                            jobName,
                            jobExecution.getStatus());

                    return;
                }

                Duration duration = Duration.between(startTime, endTime);

                meterRegistry.timer("mopl.batch.job.duration", "job", jobName).record(duration);

                if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
                    meterRegistry.counter("mopl.batch.job.success", "job", jobName).increment();
                } else {
                    meterRegistry.counter("mopl.batch.job.failure", "job", jobName).increment();
                }

                log.debug("Job 종료: {} - 상태: {}, 소요시간: {}s", jobName, jobExecution.getStatus(), duration);
            }
        };
    }
}
