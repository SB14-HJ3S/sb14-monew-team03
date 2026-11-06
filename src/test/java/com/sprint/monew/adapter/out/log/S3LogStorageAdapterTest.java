package com.sprint.monew.adapter.out.log;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sprint.monew.application.log.LogBackupApplication;
import com.sprint.monew.global.config.S3Properties;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.ArticleErrorType;
import com.sprint.monew.global.util.backup.ArticleBackupGzipCompressor;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

@ExtendWith(MockitoExtension.class)
class S3LogStorageAdapterTest {

    private static final LocalDate DATE = LocalDate.of(2026, 10, 31);
    @Mock private S3Client s3Client;
    @TempDir Path logDirectory;

    @ParameterizedTest
    @CsvSource({"200, true", "403, false", "500, false"})
    @DisplayName("묶음의 S3 PUT 응답이 성공한 경우에만 성공을 반환한다")
    void confirmUploadFromHttpResponse(int statusCode, boolean expectedSuccess) throws Exception {
        Path file = Files.createFile(logDirectory.resolve("monew.log.2026-10-31.0.log"));
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(response(statusCode));

        assertThat(adapter().upload(DATE, 1L, List.of(file))).isEqualTo(expectedSuccess);
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    static Stream<SdkException> uploadFailures() {
        return Stream.of(
                S3Exception.builder().statusCode(503).message("service unavailable").build(),
                SdkClientException.create("connection failed"));
    }

    @ParameterizedTest
    @MethodSource("uploadFailures")
    @DisplayName("S3 서비스 오류와 클라이언트 오류를 묶음 업로드 실패로 반환한다")
    void returnFalseOnSdkFailure(SdkException failure) throws Exception {
        Path file = Files.createFile(logDirectory.resolve("monew.log.2026-10-31.0.log"));
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(failure);

        assertThat(adapter().upload(DATE, 1L, List.of(file))).isFalse();
    }

    @Test
    @DisplayName("여러 로그를 한 번 업로드하고 tar.gz에서 각 파일명과 원본 바이트를 복원할 수 있다")
    void uploadOneArchivePreservingAllFiles() throws Exception {
        Path first =
                Files.writeString(
                        logDirectory.resolve("monew.log.2026-10-31.0.log"),
                        "INFO 로그 백업 테스트\nERROR 여러 줄 오류\n    at example.run(Example.java:1)\n");
        Path empty = Files.createFile(logDirectory.resolve("monew.log.2026-10-31.1.log"));
        Path binary =
                Files.write(
                        logDirectory.resolve("긴이름-" + "x".repeat(120) + ".log"),
                        new byte[] {0, (byte) 255, 13, 10, 1});
        List<Path> files = List.of(first, empty, binary);
        Map<String, byte[]> original = new LinkedHashMap<>();
        for (Path file : files) {
            original.put(file.getFileName().toString(), Files.readAllBytes(file));
        }
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(response(200));

        assertThat(adapter().upload(DATE, 2L, files)).isTrue();

        ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
        ArgumentCaptor<RequestBody> body = ArgumentCaptor.forClass(RequestBody.class);
        verify(s3Client).putObject(request.capture(), body.capture());
        assertThat(request.getValue().bucket()).isEqualTo("test-bucket");
        assertThat(request.getValue().key()).isEqualTo("logs/2026-10-31/logs-2.tar.gz");
        assertThat(request.getValue().contentType()).isEqualTo("application/gzip");
        Map<String, byte[]> restored = extract(body.getValue());
        assertThat(restored.keySet()).containsExactlyElementsOf(original.keySet());
        for (Path file : files) {
            String name = file.getFileName().toString();
            assertThat(restored.get(name)).isEqualTo(original.get(name));
            assertThat(Files.readAllBytes(file)).isEqualTo(original.get(name));
        }
        try (Stream<Path> remaining = Files.list(logDirectory)) {
            assertThat(remaining.toList()).containsExactlyInAnyOrderElementsOf(files);
        }
    }

    @Test
    @DisplayName("묶음 업로드 실패 후 재시도하면 같은 S3 키에 한 묶음 전체를 저장한다")
    void retryWholeArchiveWithSameKey() throws Exception {
        Path first = Files.writeString(logDirectory.resolve("monew.log.2026-10-31.0.log"), "first");
        Path second =
                Files.writeString(logDirectory.resolve("monew.log.2026-10-31.1.log"), "second");
        List<Path> files = List.of(first, second);
        var application =
                new LogBackupApplication(adapter(), logDirectory.resolve("monew.log").toString());
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(S3Exception.builder().statusCode(503).build())
                .thenReturn(response(200));

        var failed = application.backup(DATE, 2L, files);
        var retried = application.backup(DATE, 2L, files);

        assertThat(failed.totalFileCount()).isEqualTo(2);
        assertThat(failed.uploadedFileCount()).isZero();
        assertThat(retried.totalFileCount()).isEqualTo(2);
        assertThat(retried.uploadedFileCount()).isEqualTo(2);
        ArgumentCaptor<PutObjectRequest> requests = ArgumentCaptor.forClass(PutObjectRequest.class);
        ArgumentCaptor<RequestBody> bodies = ArgumentCaptor.forClass(RequestBody.class);
        verify(s3Client, times(2)).putObject(requests.capture(), bodies.capture());
        assertThat(requests.getAllValues())
                .extracting(PutObjectRequest::key)
                .containsOnly("logs/2026-10-31/logs-2.tar.gz");
        for (RequestBody body : bodies.getAllValues()) {
            Map<String, byte[]> restored = extract(body);
            assertThat(restored.keySet())
                    .containsExactly(
                            first.getFileName().toString(), second.getFileName().toString());
            assertThat(restored.get(first.getFileName().toString()))
                    .isEqualTo("first".getBytes(StandardCharsets.UTF_8));
            assertThat(restored.get(second.getFileName().toString()))
                    .isEqualTo("second".getBytes(StandardCharsets.UTF_8));
        }
    }

    @Test
    @DisplayName("날짜와 part 번호가 다르면 서로 다른 묶음 키로 저장한다")
    void separateDatesAndParts() throws Exception {
        Path file = Files.createFile(logDirectory.resolve("monew.log.2026-10-31.0.log"));
        var adapter = adapter();
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(response(200));
        adapter.upload(DATE, 1L, List.of(file));
        adapter.upload(DATE, 2L, List.of(file));
        adapter.upload(DATE.plusDays(1), 1L, List.of(file));
        ArgumentCaptor<PutObjectRequest> requests = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client, times(3)).putObject(requests.capture(), any(RequestBody.class));
        assertThat(requests.getAllValues())
                .extracting(PutObjectRequest::key)
                .containsExactly(
                        "logs/2026-10-31/logs-1.tar.gz",
                        "logs/2026-10-31/logs-2.tar.gz",
                        "logs/2026-11-01/logs-1.tar.gz");
    }

    @Test
    @DisplayName("묶음 중 한 파일이라도 읽을 수 없으면 S3에 업로드하지 않는다")
    void failWholeArchiveWhenAnyFileCannotBeRead() throws Exception {
        Path first = Files.createFile(logDirectory.resolve("monew.log.2026-10-31.0.log"));
        Path missing = logDirectory.resolve("monew.log.2026-10-31.1.log");

        assertThat(adapter().upload(DATE, 1L, List.of(first, missing))).isFalse();
        verifyNoInteractions(s3Client);
        assertThat(first).exists();
    }

    @Test
    @DisplayName("기존 기사 압축 유틸에서 오류가 발생하면 묶음 업로드 실패를 반환한다")
    void returnFalseWhenCompressionFails() throws Exception {
        Path file = Files.createFile(logDirectory.resolve("monew.log.2026-10-31.0.log"));
        ArticleBackupGzipCompressor failingCompressor =
                new ArticleBackupGzipCompressor() {
                    @Override
                    public byte[] compress(byte[] content) {
                        throw new MonewException(ArticleErrorType.ARTICLE_BACKUP_ZIP_FAILED);
                    }
                };
        var adapter = new S3LogStorageAdapter(s3Client, properties(), failingCompressor);

        assertThat(adapter.upload(DATE, 1L, List.of(file))).isFalse();
        verifyNoInteractions(s3Client);
    }

    private Map<String, byte[]> extract(RequestBody body) throws Exception {
        Map<String, byte[]> result = new LinkedHashMap<>();
        try (TarArchiveInputStream tar =
                new TarArchiveInputStream(
                        new GZIPInputStream(body.contentStreamProvider().newStream()), "UTF-8")) {
            TarArchiveEntry entry;
            while ((entry = tar.getNextEntry()) != null) {
                assertThat(entry.isDirectory()).isFalse();
                result.put(entry.getName(), tar.readAllBytes());
            }
        }
        return result;
    }

    private S3LogStorageAdapter adapter() {
        return new S3LogStorageAdapter(s3Client, properties(), new ArticleBackupGzipCompressor());
    }

    private S3Properties properties() {
        S3Properties properties = new S3Properties();
        properties.setBucket("test-bucket");
        return properties;
    }

    private PutObjectResponse response(int status) {
        PutObjectResponse.Builder builder = PutObjectResponse.builder();
        builder.sdkHttpResponse(SdkHttpResponse.builder().statusCode(status).build());
        return builder.build();
    }
}
