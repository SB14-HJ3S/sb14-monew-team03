package com.sprint.monew.adapter.out.article;

import com.sprint.monew.application.article.provided.dto.ArticleBackupFile;
import com.sprint.monew.application.article.provided.dto.ArticleBackupItem;
import com.sprint.monew.application.article.provided.dto.ArticleBackupResult;
import com.sprint.monew.application.article.required.ArticleBackupStoragePort;
import com.sprint.monew.global.config.S3Properties;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.ArticleErrorType;
import com.sprint.monew.global.util.backup.ArticleBackupGzipCompressor;
import com.sprint.monew.global.util.backup.ArticleBackupJsonSerializer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

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

    @Override
    public List<ArticleBackupItem> readAll(LocalDate date) {
        String prefix = "articles/" + date + "/articles-" + date + "-";

        ListObjectsV2Request request =
                ListObjectsV2Request.builder()
                        .bucket(properties.getBucket())
                        .prefix(prefix)
                        .build();

        List<ArticleBackupItem> articles = new ArrayList<>();

        try {
            for (ListObjectsV2Response page : s3Client.listObjectsV2Paginator(request)) {
                for (S3Object object : page.contents()) {
                    if (!object.key().endsWith(".json.gz")) {
                        continue;
                    }

                    GetObjectRequest getObjectRequest =
                            GetObjectRequest.builder()
                                    .bucket(properties.getBucket())
                                    .key(object.key())
                                    .build();

                    byte[] compressed = s3Client.getObjectAsBytes(getObjectRequest).asByteArray();
                    byte[] json = gzipCompressor.decompress(compressed);
                    articles.addAll(serializer.deserialize(json));
                }
            }
        } catch (SdkException e) {
            throw new MonewException(ArticleErrorType.ARTICLE_BACKUP_READ_FAILED, e);
        }

        return List.copyOf(articles);
    }

    private void upload(String key, byte[] content) {
        PutObjectRequest request =
                PutObjectRequest.builder()
                        .bucket(properties.getBucket())
                        .key(key)
                        .contentType(CONTENT_TYPE)
                        .build();

        try {
            s3Client.putObject(request, RequestBody.fromBytes(content));
        } catch (SdkException e) {
            throw new MonewException(ArticleErrorType.ARTICLE_BACKUP_UPLOAD_FAILED, e);
        }
    }

    private String generateKey(LocalDate date, long partNumber) {
        return "articles/"
                + date
                + "/articles-"
                + date
                + "-"
                + String.format(Locale.ROOT, "%06d", partNumber)
                + ".json.gz";
    }
}
