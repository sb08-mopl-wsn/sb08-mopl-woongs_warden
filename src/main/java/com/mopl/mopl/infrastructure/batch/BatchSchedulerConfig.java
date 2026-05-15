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
    private final Job contentCollectJob;

    @Scheduled(cron = "0 0/1 * * * *", zone = "Asia/Seoul")
    public void runContentCollectJob() {
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(contentCollectJob, jobParameters);
        } catch (Exception e) {
            log.error("콘텐츠 수집 Job 실행 실패", e);
        }
    }
}
