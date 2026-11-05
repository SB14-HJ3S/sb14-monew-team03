package com.sprint.monew.adapter.out.log;

import com.sprint.monew.application.log.required.LogStoragePort;
import com.sprint.monew.global.config.S3Properties;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.util.backup.ArticleBackupGzipCompressor;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3LogStorageAdapter implements LogStoragePort {

    private static final String CONTENT_TYPE = "application/gzip";

    private final S3Client s3Client;
    private final S3Properties properties;
    private final ArticleBackupGzipCompressor gzipCompressor;

    @Override
    public boolean upload(LocalDate targetDate, long partNumber, List<Path> files) {
        String key = getObjectKey(targetDate, partNumber);
        PutObjectRequest request =
                PutObjectRequest.builder()
                        .bucket(properties.getBucket())
                        .key(key)
                        .contentType(CONTENT_TYPE)
                        .build();

        try {
            byte[] content = gzipCompressor.compress(createTar(files));
            return s3Client.putObject(request, RequestBody.fromBytes(content))
                    .sdkHttpResponse()
                    .isSuccessful();
        } catch (IOException | SdkException | MonewException e) {
            log.warn(
                    "로그 묶음 S3 업로드 실패: targetDate={}, partNumber={}, fileCount={}, errorType={}",
                    targetDate,
                    partNumber,
                    files.size(),
                    e.getClass().getSimpleName());
            return false;
        }
    }

    private byte[] createTar(List<Path> files) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (TarArchiveOutputStream tar = new TarArchiveOutputStream(output, "UTF-8")) {
            tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
            tar.setAddPaxHeadersForNonAsciiNames(true);
            for (Path file : files) {
                tar.putArchiveEntry(
                        new TarArchiveEntry(file.toFile(), file.getFileName().toString()));
                Files.copy(file, tar);
                tar.closeArchiveEntry();
            }
        }
        return output.toByteArray();
    }

    private String getObjectKey(LocalDate targetDate, long partNumber) {
        return "logs/" + targetDate + "/logs-" + partNumber + ".tar.gz";
    }
}
