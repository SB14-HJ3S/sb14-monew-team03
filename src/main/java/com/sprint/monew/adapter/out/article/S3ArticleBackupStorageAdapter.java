package com.sprint.monew.adapter.out.article;

import com.sprint.monew.application.article.provided.dto.ArticleBackupFile;
import com.sprint.monew.application.article.provided.dto.ArticleBackupResult;
import com.sprint.monew.application.article.required.ArticleBackupStoragePort;
import com.sprint.monew.global.config.S3Properties;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.ArticleErrorType;
import java.time.LocalDate;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Component
@RequiredArgsConstructor
public class S3ArticleBackupStorageAdapter implements ArticleBackupStoragePort {

    private static final String CONTENT_TYPE = "application/gzip";

    private final S3Client s3Client;
    private final S3Properties properties;
    private final ArticleBackupJsonSerializer serializer;
    private final ArticleBackupGzipCompressor gzipCompressor;

    @Override
    public ArticleBackupResult save(long partNumber, ArticleBackupFile backupFile) {
        String key = generateKey(backupFile.backupDate(), partNumber);
        byte[] json = serializer.serialize(backupFile);
        byte[] content = gzipCompressor.compress(json);

        upload(key, content);

        return ArticleBackupResult.of(key, backupFile);
    }

    private void upload(String key, byte[] content) {
        PutObjectRequest request =
                PutObjectRequest.builder()
                        .bucket(properties.getBucket())
                        .key(key)
                        .contentType(CONTENT_TYPE)
                        .ifNoneMatch("*")
                        .build();

        try {
            s3Client.putObject(request, RequestBody.fromBytes(content));
        } catch (SdkException e) {
            throw new MonewException(
                    ArticleErrorType.ARTICLE_BACKUP_UPLOAD_FAILED, Map.of("key", key), e);
        }
    }

    private String generateKey(LocalDate date, long partNumber) {
        return "articles/" + date + "/articles-" + date + "-" + partNumber + ".json.gz";
    }
}
