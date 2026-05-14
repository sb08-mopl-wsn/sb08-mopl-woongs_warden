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
import java.util.List;
import java.util.stream.Stream;

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

    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    public void uploadDailyLog() throws Exception {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        String prefix = String.format("mopl-%s.", yesterday);
        Path baseDir = Path.of(logDir);

        if (!Files.exists(baseDir)) {
            log.warn("로그 디렉토리 없음: {}", baseDir);
            return;
        }

        // 패키지 전체를 순회하면서 조건(mopl-날짜.log)에 맞는 모든 일반 파일(isRegularFile)
        try (Stream<Path> stream = Files.list(baseDir)) {
            List<Path> targets = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith(prefix))
                    .filter(path -> path.getFileName().toString().endsWith(".log"))
                    .toList();

            if (targets.isEmpty()) {
                log.warn("업로드할 로그 파일 없음: {}*.log", prefix);
                return;
            }

            // 모든 파일의 이름으로 업로드
            for (Path target : targets) {
                try {
                    s3Client.putObject(
                            PutObjectRequest.builder()
                                    .bucket(bucket)
                                    .key("logs/" + target.getFileName())
                                    .build(),
                            target
                    );
                    log.info("로그 업로드 완료: {}", target.getFileName());
                } catch (Exception e) {
                    log.error("로그 업로드 실패: {}", target.getFileName(), e);
                }
            }
        }
    }
}
