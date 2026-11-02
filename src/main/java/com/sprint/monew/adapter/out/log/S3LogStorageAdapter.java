package com.sprint.monew.adapter.out.log;

import com.sprint.monew.application.log.required.LogStoragePort;
import com.sprint.monew.global.config.S3Properties;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3LogStorageAdapter implements LogStoragePort {

    private final S3Client s3Client;
    private final S3Properties properties;

    @Override
    public boolean upload(Path file) {
        String key = getObjectKey(file);
        PutObjectRequest request =
                PutObjectRequest.builder()
                        .bucket(properties.getBucket())
                        .key(key)
                        .contentType("text/plain; charset=UTF-8")
                        .build();

        try {
            return s3Client.putObject(request, RequestBody.fromFile(file))
                    .sdkHttpResponse()
                    .isSuccessful();
        } catch (SdkException e) {
            log.warn(
                    "로그 S3 업로드 실패: fileName={}, errorType={}",
                    file.getFileName(),
                    e.getClass().getSimpleName());
            return false;
        }
    }

    private String getObjectKey(Path file) {
        return "logs/" + file.getFileName();
    }
}
