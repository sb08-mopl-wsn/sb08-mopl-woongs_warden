package com.mopl.mopl.infrastructure.s3;

import com.mopl.mopl.infrastructure.s3.exception.S3DeleteException;
import com.mopl.mopl.infrastructure.s3.exception.S3UploadException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Component
public class S3ImageStorage
{
    private final S3Client s3Client;
    private final String bucket;
    private final String cdnUrl;

    public S3ImageStorage(S3Client s3Client,
                          @Value("${cloud.aws.s3.bucket}") String bucket,
                          @Value("${cloud.aws.s3.cdn-url}") String cdnUrl)
    {
        this.s3Client = s3Client;
        this.bucket = bucket;
        this.cdnUrl = cdnUrl;
    }

    /**
     * 이미지 파일에 S3에 업로드한다.
     *
     * @param file 업로드할 이미지 파일
     * @param directory 저장 경로
     * @return 업로드된 S3 키
     * @throws S3UploadException S3 업로드 실패 시
     */
    public String upload(MultipartFile file, String directory) {
        String key = generateKey(file, directory);

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            try (InputStream inputStream = file.getInputStream()) {
                s3Client.putObject(request, RequestBody.fromInputStream(inputStream, file.getSize()));
            }

            log.debug("[S3ImageStorage] Uploaded file {} to bucket {}]", file.getOriginalFilename(), bucket);
        } catch (IOException | SdkException e) {
            log.warn("[S3ImageStorage] Uploaded file {} to bucket {} failed", file.getOriginalFilename(), bucket, e);
            throw new S3UploadException();
        }

        return key;
    }

    /**
     * 이미지 파일을 S3에서 삭제한다.
     *
     * @param key S3 파일 키
     */
    public void delete(String key) {
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();
            s3Client.deleteObject(request);
            log.debug("[S3ImageStorage] Deleted file {} from bucket {}", key, bucket);
        } catch (SdkException e) {
            log.warn("[S3ImageStorage] Deleted file {} from bucket {} failed", key, bucket, e);
            throw new S3DeleteException();
        }
    }

    /**
     * 저장된 key로 공개 URL을 생성한다.
     *
     * @param key S3 파일 키
     * @return CDN URL
     */
    public String getPublicUrl(String key) {
        return cdnUrl + "/" + key;
    }

    private String generateKey(MultipartFile file, String directory) {
        String originalFilename = file.getOriginalFilename();
        String extension = "";

        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        return directory + "/" + UUID.randomUUID() + extension;
    }
}