package com.sprint.monew.adapter.out.s3.article;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sprint.monew.application.article.provided.service.dto.ArticleBackupFile;
import com.sprint.monew.global.config.S3Properties;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.util.backup.ArticleBackupGzipCompressor;
import com.sprint.monew.global.util.backup.ArticleBackupJsonSerializer;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;

@ExtendWith(MockitoExtension.class)
class S3ArticleBackupStorageAdapterTest {
    @Mock private S3Client s3Client;
    @Mock private S3Properties properties;
    @Mock private ArticleBackupJsonSerializer serializer;
    @Mock private ArticleBackupGzipCompressor gzipCompressor;
    @InjectMocks private S3ArticleBackupStorageAdapter adapter;

    @Test
    @DisplayName("발행일 폴더에 발행일과 청크 번호를 포함한 파일로 저장한다")
    void savesUnderPublishedDate() {
        // given
        LocalDate publishedDate = LocalDate.of(2022, 10, 10);
        ArticleBackupFile file = ArticleBackupFile.of(publishedDate, List.of());
        byte[] json = new byte[] {1};
        when(properties.getBucket()).thenReturn("test-bucket");
        when(serializer.serialize(file)).thenReturn(json);
        when(gzipCompressor.compress(json)).thenReturn(json);

        stubPages(ListObjectsV2Response.builder().build());

        // when
        var result = adapter.save(file);

        // then
        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        org.mockito.Mockito.verify(s3Client).putObject(captor.capture(), any(RequestBody.class));
        assertThat(captor.getValue().key())
                .isEqualTo("articles/2022-10-10/articles-2022-10-10-000001.json.gz");
        assertThat(result.backupDate()).isEqualTo(publishedDate);
    }

    @Test
    @DisplayName("여러 페이지의 최대 파일 번호 다음으로 저장한다")
    void savesAfterLargestNumber() {
        // given
        LocalDate date = LocalDate.of(2022, 10, 10);
        ArticleBackupFile file = ArticleBackupFile.of(date, List.of());
        byte[] content = new byte[] {1};
        when(properties.getBucket()).thenReturn("test-bucket");
        when(serializer.serialize(file)).thenReturn(content);
        when(gzipCompressor.compress(content)).thenReturn(content);
        String prefix = "articles/2022-10-10/articles-2022-10-10-";
        stubPages(
                ListObjectsV2Response.builder()
                        .contents(S3Object.builder().key(prefix + "000001.json.gz").build())
                        .build(),
                ListObjectsV2Response.builder()
                        .contents(
                                S3Object.builder().key(prefix + "000008.json.gz").build(),
                                S3Object.builder()
                                        .key(prefix + "collected-2026-11-06-999999.json.gz")
                                        .build())
                        .build());

        // when
        var result = adapter.save(file);

        // then
        assertThat(result.key()).isEqualTo(prefix + "000009.json.gz");
        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(captor.capture(), any(RequestBody.class));
        assertThat(captor.getValue().key()).isEqualTo(result.key());
    }

    @Test
    @DisplayName("업로드 실패는 재시도 없이 기존 업로드 오류로 전달한다")
    void propagatesUploadFailure() {
        // given
        ArticleBackupFile file = ArticleBackupFile.of(LocalDate.of(2022, 10, 10), List.of());
        byte[] content = new byte[] {1};
        when(properties.getBucket()).thenReturn("test-bucket");
        when(serializer.serialize(file)).thenReturn(content);
        when(gzipCompressor.compress(content)).thenReturn(content);
        stubPages(ListObjectsV2Response.builder().build());
        var cause = S3Exception.builder().statusCode(500).build();
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(cause);

        // when
        Throwable failure = catchThrowable(() -> adapter.save(file));

        // then
        assertThat(failure).isInstanceOf(MonewException.class).hasCause(cause);
        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    private void stubPages(ListObjectsV2Response... responses) {
        ListObjectsV2Iterable pages = mock(ListObjectsV2Iterable.class);
        when(pages.iterator()).thenAnswer(invocation -> List.of(responses).iterator());
        when(s3Client.listObjectsV2Paginator(any(ListObjectsV2Request.class))).thenReturn(pages);
    }
}
