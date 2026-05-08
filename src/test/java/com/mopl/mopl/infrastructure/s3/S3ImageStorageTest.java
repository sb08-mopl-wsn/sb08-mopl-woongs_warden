package com.mopl.mopl.infrastructure.s3;

import com.mopl.mopl.infrastructure.s3.exception.S3DeleteException;
import com.mopl.mopl.infrastructure.s3.exception.S3UploadException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("S3ImageStorage Unit Test")
class S3ImageStorageTest
{
    @Mock private S3Client s3Client;

    private S3ImageStorage s3ImageStorage;

    private static final String BUCKET = "test-bucket";
    private static final String CDN_URL = "https://cdn.mopl.com";

    @BeforeEach
    void setUp() {
        s3ImageStorage = new S3ImageStorage(s3Client, BUCKET, CDN_URL);
    }
    
    @Nested
    @DisplayName("이미지 업로드")
    class Upload {
        @Test
        @DisplayName("이미지 업로드 성공 시 S3 key를 반환한다.")
        void givenValidFile_whenUpload_thenReturnsKey() {
            // given
            MultipartFile file = new MockMultipartFile(
                    "image", "test.jpg", "image/jpeg", "image-data".getBytes()
            );

            given(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class))).willReturn(PutObjectResponse.builder().build());

            // when
            String key = s3ImageStorage.upload(file, "thumbnails");

            // then
            assertThat(key).startsWith("thumbnails/");
            assertThat(key).endsWith(".jpg");

            then(s3Client).should().putObject(any(PutObjectRequest.class), any(RequestBody.class));
        }
        
        @Test
        @DisplayName("확장자가 없는 파일도 업로드할 수 있다.")
        void givenFileWithoutExtension_whenUpload_thenReturnsKeyWithoutExtension() {
            // given
            MultipartFile file = new MockMultipartFile(
                    "image", "noext", "image/png", "image-data".getBytes()
            );

            given(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                    .willReturn(PutObjectResponse.builder().build());
            
            // when
            String key = s3ImageStorage.upload(file, "thumbnails");
            
            // then
            assertThat(key).startsWith("thumbnails/");
            assertThat(key).doesNotContain(".");
        }

        @Test
        @DisplayName("S3 업로드 실패 시 S3UploadException을 던진다")
        void givenS3Failure_whenUpload_thenThrowsS3UploadException() {
            // given
            MultipartFile file = new MockMultipartFile(
                    "image", "test.jpg", "image/jpeg", "image-data".getBytes()
            );

            given(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                    .willThrow(SdkException.builder().message("S3 error").build());

            // when & then
            assertThatThrownBy(() -> s3ImageStorage.upload(file, "thumbnails"))
                    .isInstanceOf(S3UploadException.class);
        }
    }

    @Nested
    @DisplayName("이미지 삭제")
    class Delete {
        @Test
        @DisplayName("이미지 삭제 성공")
        void givenValidKey_whenDelete_thenSuccess() {
            // given
            String key = "thumbnails/uuid.jpg";

            given(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                    .willReturn(DeleteObjectResponse.builder().build());

            // when
            s3ImageStorage.delete(key);

            // then
            then(s3Client).should().deleteObject(any(DeleteObjectRequest.class));
        }

        @Test
        @DisplayName("S3 삭제 실패 시 S3DeleteException을 던진다")
        void givenS3Failure_whenDelete_thenThrowsS3DeleteException() {
            // given
            String key = "thumbnails/uuid.jpg";

            given(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                    .willThrow(SdkException.builder().message("S3 error").build());

            // when & then
            assertThatThrownBy(() -> s3ImageStorage.delete(key))
                    .isInstanceOf(S3DeleteException.class);
        }
    }

    @Nested
    @DisplayName("CDN")
    class CDN {
        @Test
        @DisplayName("key로 CDN URL을 생성한다")
        void givenKey_whenGetPublicUrl_thenReturnsCdnUrl() {
            // given
            String key = "thumbnails/uuid.jpg";

            // when
            String url = s3ImageStorage.getPublicUrl(key);

            // then
            assertThat(url).isEqualTo("https://cdn.mopl.com/thumbnails/uuid.jpg");
        }
    }
}