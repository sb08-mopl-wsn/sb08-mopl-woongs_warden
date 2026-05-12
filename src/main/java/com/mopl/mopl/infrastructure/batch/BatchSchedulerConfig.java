package com.mopl.mopl.infrastructure.batch;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@RequiredArgsConstructor
@Configuration
@EnableScheduling
public class BatchSchedulerConfig
{
    private final JobLauncher jobLauncher;
    private final Job tmdbCollectJob;
    private final Job sportsdbCollectJob;

    @Scheduled(cron = "0 0 3 * * *")
    public void runTmdbCollectJob() throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        jobLauncher.run(tmdbCollectJob, jobParameters);
    }

    @Scheduled(cron = "0 30 3 * * *")
    public void runSportsDbCollectJob() throws Exception {
        JobParameters params = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();
        jobLauncher.run(sportsdbCollectJob, params);
    }
}
