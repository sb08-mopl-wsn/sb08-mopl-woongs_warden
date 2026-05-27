package com.mopl.mopl.infrastructure.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDate;
import java.time.ZoneId;

@Slf4j
@RequiredArgsConstructor
@Configuration
@EnableScheduling
public class BatchSchedulerConfig
{
    private final JobLauncher jobLauncher;
    private final Job contentCollectJob;

    @SchedulerLock(name = "contentCollectJob", lockAtMostFor = "10m", lockAtLeastFor = "5m")
    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    public void runContentCollectJob() {
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLocalDate("date", LocalDate.now(ZoneId.of("Asia/Seoul")))
                    .toJobParameters();

            jobLauncher.run(contentCollectJob, jobParameters);
        } catch (Exception e) {
            log.error("콘텐츠 수집 Job 실행 실패", e);
        }
    }
}
