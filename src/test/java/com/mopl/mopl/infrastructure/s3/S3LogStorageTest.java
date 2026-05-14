package com.mopl.mopl.infrastructure.s3;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("S3LogStorage Test")
class S3LogStorageTest
{
    @InjectMocks private S3LogStorage s3LogStorage;
    @Mock private S3Client s3Client;
    @TempDir Path tempDir;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(s3LogStorage, "bucket", "test-bucket");
        ReflectionTestUtils.setField(s3LogStorage, "logDir", tempDir.toString());
    }

    @Test
    @DisplayName("성공 - 전날 로그 파일이 존재하면 S3에 업로드한다")
    void givenYesterdayLogFileExists_whenUploadDailyLog_thenUploadsToS3() throws Exception {
        // given
        LocalDate date = LocalDate.of(2026, 5, 13);
        String fileName = String.format("mopl-%s.0.log", date);
        Files.createFile(tempDir.resolve(fileName));

        given(s3Client.putObject(any(PutObjectRequest.class), any(Path.class)))
                .willReturn(PutObjectResponse.builder().build());

        // when
        s3LogStorage.uploadDailyLog();

        // then
        verify(s3Client).putObject(
                argThat((PutObjectRequest req) ->
                        req.bucket().equals("test-bucket") &&
                                req.key().equals("logs/" + fileName)),
                eq(tempDir.resolve(fileName))
        );
    }

    @Test
    @DisplayName("스킵 - 전날 로그 파일이 없으면 업로드하지 않는다")
    void givenLogFileNotExists_whenUploadDailyLog_thenSkipsUpload() {
        // when
        s3LogStorage.uploadDailyLog();

        // then
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(Path.class));
    }

    @Test
    @DisplayName("실패 - S3 업로드 실패 시 예외를 던지지 않는다")
    void givenS3Fails_whenUploadDailyLog_thenDoesNotThrow() throws Exception {
        // given
        LocalDate date = LocalDate.of(2026, 5, 13);
        String fileName = String.format("mopl-%s.0.log", date);
        Files.createFile(tempDir.resolve(fileName));

        given(s3Client.putObject(any(PutObjectRequest.class), any(Path.class)))
                .willThrow(S3Exception.builder().message("upload failed").build());

        // when & then
        assertThatNoException().isThrownBy(() -> s3LogStorage.uploadDailyLog());
    }
}