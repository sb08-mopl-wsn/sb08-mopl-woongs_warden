package com.mopl.mopl.infrastructure.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@RequiredArgsConstructor
@Configuration
@EnableScheduling
public class BatchSchedulerConfig
{
    private final JobLauncher jobLauncher;
    private final Job tmdbCollectJob;
    private final Job sportsdbCollectJob;

    @Scheduled(cron = "0 0 3 * * *")
    public void runTmdbCollectJob() {
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(tmdbCollectJob, jobParameters);
        } catch (Exception e) {
            log.error("TMDB 콘텐츠 수집 Job 실행 실패", e);
        }
    }

    @Scheduled(cron = "0 30 3 * * *")
    public void runSportsDbCollectJob() {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();
            jobLauncher.run(sportsdbCollectJob, params);
        } catch (Exception e) {
            log.error("Sportsdb 콘텐츠 수집 Job 실행 실패", e);
        }
    }
}
