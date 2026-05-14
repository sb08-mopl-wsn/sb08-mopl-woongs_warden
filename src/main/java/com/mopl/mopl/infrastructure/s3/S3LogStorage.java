package com.mopl.mopl.infrastructure.s3;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

@RequiredArgsConstructor
@Slf4j
@Component
public class S3LogStorage
{
    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${logging.file.path:.logs}")
    private String logDir;

    private final S3Client s3Client;

    @Scheduled(cron = "0 0 4 * * *")
    public void uploadDailyLog() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        String fileName = String.format("mopl-%s.0.log", yesterday);
        Path logPath = Path.of(logDir, fileName);

        if (!Files.exists(logPath)) {
            log.warn("업로드할 로그 파일 없음: {}", fileName);
            return;
        }

        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key("logs/" + fileName)
                            .build(),
                    logPath
            );
            log.info("로그 업로드 완료: {}", fileName);
        } catch (Exception e) {
            log.error("로그 업로드 실패: {}", fileName, e);
        }
    }
}
