package com.sprint.monew.adapter.out.log;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.sprint.monew.application.log.LogBackupApplication;
import com.sprint.monew.application.log.provided.dto.LogBackupResult;
import com.sprint.monew.global.config.S3Properties;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
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

    @Mock private S3Client s3Client;
    @TempDir Path logDirectory;

    @ParameterizedTest
    @CsvSource({"200, true", "403, false", "500, false"})
    @DisplayName("S3 PUT의 HTTP 응답이 성공한 경우에만 업로드 성공을 반환한다")
    void confirmUploadFromHttpResponse(int statusCode, boolean expectedSuccess) throws Exception {
        Path file = Files.createFile(logDirectory.resolve("monew.log.2026-10-31.0.log"));
        S3Properties properties = new S3Properties();
        properties.setBucket("test-bucket");
        S3LogStorageAdapter adapter = new S3LogStorageAdapter(s3Client, properties);
        PutObjectResponse.Builder responseBuilder = PutObjectResponse.builder();
        responseBuilder.sdkHttpResponse(SdkHttpResponse.builder().statusCode(statusCode).build());
        PutObjectResponse response = responseBuilder.build();
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(response);

        assertThat(adapter.upload(file)).isEqualTo(expectedSuccess);
    }

    static Stream<SdkException> uploadFailures() {
        return Stream.of(
                S3Exception.builder().statusCode(503).message("service unavailable").build(),
                SdkClientException.create("connection failed"));
    }

    @ParameterizedTest
    @MethodSource("uploadFailures")
    @DisplayName("S3 서비스 오류와 클라이언트 오류를 업로드 실패로 반환한다")
    void returnFalseOnSdkFailure(SdkException failure) throws Exception {
        Path file = Files.createFile(logDirectory.resolve("monew.log.2026-10-31.0.log"));
        S3Properties properties = new S3Properties();
        properties.setBucket("test-bucket");
        S3LogStorageAdapter adapter = new S3LogStorageAdapter(s3Client, properties);
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(failure);

        assertThat(adapter.upload(file)).isFalse();
    }

    @Test
    @DisplayName("파일 업로드에 실패해도 다음 파일을 처리해 부분 성공 결과를 반환한다")
    void continueBackupAfterUploadFailure() throws Exception {
        for (int index = 0; index < 3; index++) {
            Files.createFile(logDirectory.resolve("monew.log.2026-10-31." + index + ".log"));
        }
        S3Properties properties = new S3Properties();
        properties.setBucket("test-bucket");
        S3LogStorageAdapter adapter = new S3LogStorageAdapter(s3Client, properties);
        LogBackupApplication application =
                new LogBackupApplication(adapter, logDirectory.resolve("monew.log").toString());
        PutObjectResponse.Builder responseBuilder = PutObjectResponse.builder();
        responseBuilder.sdkHttpResponse(SdkHttpResponse.builder().statusCode(200).build());
        PutObjectResponse response = responseBuilder.build();
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(response)
                .thenThrow(S3Exception.builder().statusCode(503).build())
                .thenReturn(response);

        LogBackupResult result = application.backup(LocalDate.of(2026, 10, 31));

        assertThat(result.totalFileCount()).isEqualTo(3);
        assertThat(result.uploadedFileCount()).isEqualTo(2);
    }
}
