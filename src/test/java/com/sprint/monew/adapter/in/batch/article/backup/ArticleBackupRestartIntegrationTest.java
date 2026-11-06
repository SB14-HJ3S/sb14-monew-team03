package com.sprint.monew.adapter.in.batch.article.backup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.sprint.monew.application.article.provided.ArticleBackupUseCase;
import com.sprint.monew.application.article.provided.dto.ArticleBackupResult;
import com.sprint.monew.application.article.provided.dto.ArticleResult;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.ArticleErrorType;
import com.sprint.monew.global.type.ArticleSourceType;
import com.sprint.monew.support.IntegrationTest;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@IntegrationTest
@TestPropertySource(properties = "monew.article.limit=2")
class ArticleBackupRestartIntegrationTest {

    private static final int LIMIT = 2;

    @MockitoBean private ArticleBackupUseCase useCase;
    @Autowired private JobLauncher jobLauncher;
    @Autowired private JobExplorer jobExplorer;
    @Autowired private MeterRegistry registry;

    @Autowired
    @Qualifier("articleBackupJob")
    private Job job;

    @Test
    @DisplayName("두 번째 청크 업로드가 실패하면 중단하고 같은 날짜의 재시작은 실패한 파일부터 이어간다")
    void restartFromLastCommittedChunk() throws Exception {
        LocalDate targetDate = LocalDate.of(2000, 1, 2);
        JobParameters parameters = parameters(targetDate);
        List<ArticleResult> first = List.of(article(), article());
        List<ArticleResult> second = List.of(article());
        List<ArticleResult> third = List.of(article());
        UUID firstLastId = first.get(1).id();
        UUID secondLastId = second.get(0).id();
        when(useCase.readArticles(targetDate, null, LIMIT)).thenReturn(first);
        when(useCase.readArticles(targetDate, firstLastId, LIMIT)).thenReturn(second);
        when(useCase.readArticles(targetDate, secondLastId, LIMIT)).thenReturn(third);
        when(useCase.readArticles(targetDate, third.get(0).id(), LIMIT)).thenReturn(List.of());
        double uploadedBefore = uploadedArticles();
        when(useCase.backupChunk(targetDate, 1L, first))
                .thenAnswer(
                        invocation -> {
                            assertThat(uploadedArticles()).isEqualTo(uploadedBefore);
                            return result(targetDate, 1L, first);
                        });
        MonewException failure = new MonewException(ArticleErrorType.ARTICLE_BACKUP_UPLOAD_FAILED);
        when(useCase.backupChunk(targetDate, 2L, second))
                .thenAnswer(
                        invocation -> {
                            assertThat(uploadedArticles()).isEqualTo(uploadedBefore + first.size());
                            throw failure;
                        })
                .thenReturn(result(targetDate, 2L, second));
        when(useCase.backupChunk(targetDate, 3L, third)).thenReturn(result(targetDate, 3L, third));

        JobExecution failed = jobLauncher.run(job, parameters);

        assertThat(failed.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(failed.getAllFailureExceptions()).contains(failure);
        assertThat(uploadedArticles()).isEqualTo(uploadedBefore + first.size());
        verify(useCase, never()).readArticles(targetDate, secondLastId, LIMIT);
        verify(useCase, never()).backupChunk(targetDate, 3L, third);
        assertThat(jobExplorer.getJobExecution(failed.getId()).getStatus())
                .isEqualTo(BatchStatus.FAILED);
        assertThat(jobExplorer.getJobExecution(failed.getId()).getStepExecutions())
                .singleElement()
                .satisfies(
                        step -> {
                            assertThat(step.getStatus()).isEqualTo(BatchStatus.FAILED);
                            assertThat(step.getWriteCount()).isEqualTo(1);
                            assertThat(
                                            step.getExecutionContext()
                                                    .getString(ArticleBackupReader.AFTER_ID))
                                    .isEqualTo(firstLastId.toString());
                            assertThat(step.getExecutionContext().getInt(ArticleBackupReader.LIMIT))
                                    .isEqualTo(LIMIT);
                            assertThat(
                                            step.getExecutionContext()
                                                    .getLong(ArticleBackupWriter.NEXT_PART_NUMBER))
                                    .isEqualTo(2L);
                            assertThat(step.getExitStatus().getExitDescription())
                                    .contains(MonewException.class.getName());
                        });

        JobExecution restarted = jobLauncher.run(job, parameters);

        assertThat(restarted.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(restarted.getJobInstance().getInstanceId())
                .isEqualTo(failed.getJobInstance().getInstanceId());
        assertThat(restarted.getId()).isNotEqualTo(failed.getId());
        assertThat(uploadedArticles())
                .isEqualTo(uploadedBefore + first.size() + second.size() + third.size());
        verify(useCase).readArticles(targetDate, null, LIMIT);
        verify(useCase).backupChunk(targetDate, 1L, first);
        verify(useCase, times(2)).backupChunk(targetDate, 2L, second);
        verify(useCase).backupChunk(targetDate, 3L, third);
    }

    @Test
    @DisplayName("첫 청크 업로드 실패도 기록하고 재시작하면 커서 없이 파일 1번부터 다시 처리한다")
    void restartAfterFirstChunkFails() throws Exception {
        LocalDate targetDate = LocalDate.of(2000, 2, 2);
        JobParameters parameters = parameters(targetDate);
        List<ArticleResult> page = List.of(article());
        when(useCase.readArticles(targetDate, null, LIMIT)).thenReturn(page);
        when(useCase.readArticles(targetDate, page.get(0).id(), LIMIT)).thenReturn(List.of());
        when(useCase.backupChunk(targetDate, 1L, page))
                .thenThrow(new MonewException(ArticleErrorType.ARTICLE_BACKUP_UPLOAD_FAILED))
                .thenReturn(result(targetDate, 1L, page));
        double uploadedBefore = uploadedArticles();

        JobExecution failed = jobLauncher.run(job, parameters);

        assertThat(failed.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(uploadedArticles()).isEqualTo(uploadedBefore);
        assertThat(jobExplorer.getJobExecution(failed.getId()).getStepExecutions())
                .singleElement()
                .satisfies(
                        step -> {
                            assertThat(
                                            step.getExecutionContext()
                                                    .containsKey(ArticleBackupReader.AFTER_ID))
                                    .isFalse();
                            assertThat(
                                            step.getExecutionContext()
                                                    .getLong(ArticleBackupWriter.NEXT_PART_NUMBER))
                                    .isEqualTo(1L);
                        });

        JobExecution restarted = jobLauncher.run(job, parameters);

        assertThat(restarted.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(uploadedArticles()).isEqualTo(uploadedBefore + page.size());
        verify(useCase, times(2)).readArticles(targetDate, null, LIMIT);
        verify(useCase, times(2)).backupChunk(targetDate, 1L, page);
    }

    @Test
    @DisplayName("첫 청크 커밋 후 다음 조회가 실패하면 마지막 커서부터 재시작하고 성공분은 중복 업로드하지 않는다")
    void restartAfterReadFailsFollowingCommittedChunk() throws Exception {
        LocalDate targetDate = LocalDate.of(2000, 2, 3);
        JobParameters parameters = parameters(targetDate);
        List<ArticleResult> first = List.of(article(), article());
        List<ArticleResult> second = List.of(article());
        UUID firstLastId = first.get(1).id();
        IllegalStateException failure = new IllegalStateException("다음 페이지 조회 실패");
        when(useCase.readArticles(targetDate, null, LIMIT)).thenReturn(first);
        when(useCase.readArticles(targetDate, firstLastId, LIMIT))
                .thenThrow(failure)
                .thenReturn(second);
        when(useCase.readArticles(targetDate, second.get(0).id(), LIMIT)).thenReturn(List.of());
        when(useCase.backupChunk(targetDate, 1L, first)).thenReturn(result(targetDate, 1L, first));
        when(useCase.backupChunk(targetDate, 2L, second))
                .thenReturn(result(targetDate, 2L, second));
        double uploadedBefore = uploadedArticles();

        JobExecution failed = jobLauncher.run(job, parameters);

        assertThat(failed.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(failed.getAllFailureExceptions()).contains(failure);
        assertThat(uploadedArticles()).isEqualTo(uploadedBefore + first.size());
        assertThat(jobExplorer.getJobExecution(failed.getId()).getStepExecutions())
                .singleElement()
                .satisfies(
                        step -> {
                            assertThat(
                                            step.getExecutionContext()
                                                    .getString(ArticleBackupReader.AFTER_ID))
                                    .isEqualTo(firstLastId.toString());
                            assertThat(
                                            step.getExecutionContext()
                                                    .getLong(ArticleBackupWriter.NEXT_PART_NUMBER))
                                    .isEqualTo(2L);
                        });
        verify(useCase, never()).backupChunk(targetDate, 2L, second);

        JobExecution restarted = jobLauncher.run(job, parameters);

        assertThat(restarted.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(restarted.getJobInstance().getInstanceId())
                .isEqualTo(failed.getJobInstance().getInstanceId());
        assertThat(uploadedArticles()).isEqualTo(uploadedBefore + first.size() + second.size());
        verify(useCase).readArticles(targetDate, null, LIMIT);
        verify(useCase, times(2)).readArticles(targetDate, firstLastId, LIMIT);
        verify(useCase).backupChunk(targetDate, 1L, first);
        verify(useCase).backupChunk(targetDate, 2L, second);
    }

    @Test
    @DisplayName("S3 업로드 후 DB 커밋이 실패하면 성공 수와 체크포인트를 확정하지 않고 같은 파일을 재처리한다")
    void restoreCheckpointAfterCommitFailure() throws Exception {
        LocalDate targetDate = LocalDate.of(2000, 3, 2);
        JobParameters parameters = parameters(targetDate);
        List<ArticleResult> page = List.of(article());
        when(useCase.readArticles(targetDate, null, LIMIT)).thenReturn(page);
        when(useCase.readArticles(targetDate, page.get(0).id(), LIMIT)).thenReturn(List.of());
        when(useCase.backupChunk(targetDate, 1L, page))
                .thenAnswer(
                        invocation -> {
                            TransactionSynchronizationManager.registerSynchronization(
                                    new TransactionSynchronization() {
                                        @Override
                                        public void beforeCommit(boolean readOnly) {
                                            throw new IllegalStateException("청크 커밋 실패");
                                        }
                                    });
                            return result(targetDate, 1L, page);
                        })
                .thenReturn(result(targetDate, 1L, page));
        double uploadedBefore = uploadedArticles();

        JobExecution failed = jobLauncher.run(job, parameters);

        assertThat(failed.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(uploadedArticles()).isEqualTo(uploadedBefore);
        assertThat(jobExplorer.getJobExecution(failed.getId()).getStepExecutions())
                .singleElement()
                .satisfies(
                        step -> {
                            assertThat(
                                            step.getExecutionContext()
                                                    .containsKey(ArticleBackupReader.AFTER_ID))
                                    .isFalse();
                            assertThat(
                                            step.getExecutionContext()
                                                    .getLong(ArticleBackupWriter.NEXT_PART_NUMBER))
                                    .isEqualTo(1L);
                        });

        JobExecution restarted = jobLauncher.run(job, parameters);

        assertThat(restarted.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(uploadedArticles()).isEqualTo(uploadedBefore + page.size());
        verify(useCase, times(2)).backupChunk(targetDate, 1L, page);
    }

    @Test
    @DisplayName("두 번째 청크 커밋 실패 시 첫 청크의 체크포인트와 성공 수를 보존하고 파일 2번부터 재시작한다")
    void preserveCommittedChunkAfterLaterCommitFailure() throws Exception {
        LocalDate targetDate = LocalDate.of(2000, 3, 3);
        JobParameters parameters = parameters(targetDate);
        List<ArticleResult> first = List.of(article(), article());
        List<ArticleResult> second = List.of(article());
        UUID firstLastId = first.get(1).id();
        when(useCase.readArticles(targetDate, null, LIMIT)).thenReturn(first);
        when(useCase.readArticles(targetDate, firstLastId, LIMIT)).thenReturn(second);
        when(useCase.readArticles(targetDate, second.get(0).id(), LIMIT)).thenReturn(List.of());
        when(useCase.backupChunk(targetDate, 1L, first)).thenReturn(result(targetDate, 1L, first));
        IllegalStateException failure = new IllegalStateException("두 번째 청크 커밋 실패");
        when(useCase.backupChunk(targetDate, 2L, second))
                .thenAnswer(
                        invocation -> {
                            TransactionSynchronizationManager.registerSynchronization(
                                    new TransactionSynchronization() {
                                        @Override
                                        public void beforeCommit(boolean readOnly) {
                                            throw failure;
                                        }
                                    });
                            return result(targetDate, 2L, second);
                        })
                .thenReturn(result(targetDate, 2L, second));
        double uploadedBefore = uploadedArticles();

        JobExecution failed = jobLauncher.run(job, parameters);

        assertThat(failed.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(failed.getAllFailureExceptions()).contains(failure);
        assertThat(uploadedArticles()).isEqualTo(uploadedBefore + first.size());
        assertThat(jobExplorer.getJobExecution(failed.getId()).getStepExecutions())
                .singleElement()
                .satisfies(
                        step -> {
                            assertThat(step.getWriteCount()).isEqualTo(1L);
                            assertThat(
                                            step.getExecutionContext()
                                                    .getString(ArticleBackupReader.AFTER_ID))
                                    .isEqualTo(firstLastId.toString());
                            assertThat(step.getExecutionContext().getInt(ArticleBackupReader.LIMIT))
                                    .isEqualTo(LIMIT);
                            assertThat(
                                            step.getExecutionContext()
                                                    .getLong(ArticleBackupWriter.NEXT_PART_NUMBER))
                                    .isEqualTo(2L);
                        });
        verify(useCase, never()).readArticles(targetDate, second.get(0).id(), LIMIT);

        JobExecution restarted = jobLauncher.run(job, parameters);

        assertThat(restarted.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(restarted.getJobInstance().getInstanceId())
                .isEqualTo(failed.getJobInstance().getInstanceId());
        assertThat(uploadedArticles()).isEqualTo(uploadedBefore + first.size() + second.size());
        verify(useCase).readArticles(targetDate, null, LIMIT);
        verify(useCase).backupChunk(targetDate, 1L, first);
        verify(useCase, times(2)).readArticles(targetDate, firstLastId, LIMIT);
        verify(useCase, times(2)).backupChunk(targetDate, 2L, second);
    }

    @Test
    @DisplayName("조회 또는 JSON 변환 실패는 배치를 중단하고 성공 기사 수에 포함하지 않는다")
    void failWithoutCountingUploadedArticles() throws Exception {
        LocalDate readDate = LocalDate.of(2000, 4, 2);
        LocalDate serializeDate = LocalDate.of(2000, 4, 3);
        when(useCase.readArticles(readDate, null, LIMIT))
                .thenThrow(new IllegalStateException("기사 조회 실패"));
        List<ArticleResult> page = List.of(article());
        when(useCase.readArticles(serializeDate, null, LIMIT)).thenReturn(page);
        when(useCase.backupChunk(serializeDate, 1L, page))
                .thenThrow(new MonewException(ArticleErrorType.JSON_CONVERSION_FAILED));
        double uploadedBefore = uploadedArticles();

        assertThat(jobLauncher.run(job, parameters(readDate)).getStatus())
                .isEqualTo(BatchStatus.FAILED);
        assertThat(jobLauncher.run(job, parameters(serializeDate)).getStatus())
                .isEqualTo(BatchStatus.FAILED);

        assertThat(uploadedArticles()).isEqualTo(uploadedBefore);
        verify(useCase, never()).backupChunk(eq(readDate), anyLong(), any());
    }

    @Test
    @DisplayName("백업 대상이 없으면 성공 기사 수는 증가하지 않는다")
    void doNotCountEmptyBackup() throws Exception {
        LocalDate targetDate = LocalDate.of(2000, 5, 2);
        when(useCase.readArticles(eq(targetDate), isNull(), eq(LIMIT))).thenReturn(List.of());
        double uploadedBefore = uploadedArticles();

        assertThat(jobLauncher.run(job, parameters(targetDate)).getStatus())
                .isEqualTo(BatchStatus.COMPLETED);

        assertThat(uploadedArticles()).isEqualTo(uploadedBefore);
        verify(useCase, never()).backupChunk(any(), anyLong(), any());
    }

    @Test
    @DisplayName("완료된 날짜를 다시 실행하면 조회와 업로드 및 성공 수 집계를 반복하지 않는다")
    void rejectRerunOfCompletedDate() throws Exception {
        LocalDate targetDate = LocalDate.of(2000, 6, 2);
        JobParameters parameters = parameters(targetDate);
        List<ArticleResult> page = List.of(article());
        when(useCase.readArticles(targetDate, null, LIMIT)).thenReturn(page);
        when(useCase.readArticles(targetDate, page.get(0).id(), LIMIT)).thenReturn(List.of());
        when(useCase.backupChunk(targetDate, 1L, page)).thenReturn(result(targetDate, 1L, page));
        double uploadedBefore = uploadedArticles();
        JobExecution completed = jobLauncher.run(job, parameters);
        assertThat(completed.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        assertThatThrownBy(() -> jobLauncher.run(job, parameters))
                .isInstanceOf(JobInstanceAlreadyCompleteException.class);

        assertThat(uploadedArticles()).isEqualTo(uploadedBefore + page.size());
        verify(useCase).readArticles(targetDate, null, LIMIT);
        verify(useCase).readArticles(targetDate, page.get(0).id(), LIMIT);
        verify(useCase).backupChunk(targetDate, 1L, page);
        verifyNoMoreInteractions(useCase);
    }

    @Test
    @DisplayName("이전 날짜 백업 완료 후 다른 날짜는 커서 없이 파일 1번부터 시작하고 성공 수를 별도로 더한다")
    void startNextDateWithFreshState() throws Exception {
        LocalDate firstDate = LocalDate.of(2000, 7, 2);
        LocalDate nextDate = firstDate.plusDays(1);
        List<ArticleResult> first = List.of(article(), article());
        List<ArticleResult> last = List.of(article());
        List<ArticleResult> nextDay = List.of(article());
        when(useCase.readArticles(firstDate, null, LIMIT)).thenReturn(first);
        when(useCase.readArticles(firstDate, first.get(1).id(), LIMIT)).thenReturn(last);
        when(useCase.readArticles(firstDate, last.get(0).id(), LIMIT)).thenReturn(List.of());
        when(useCase.backupChunk(firstDate, 1L, first)).thenReturn(result(firstDate, 1L, first));
        when(useCase.backupChunk(firstDate, 2L, last)).thenReturn(result(firstDate, 2L, last));
        when(useCase.readArticles(nextDate, null, LIMIT)).thenReturn(nextDay);
        when(useCase.readArticles(nextDate, nextDay.get(0).id(), LIMIT)).thenReturn(List.of());
        when(useCase.backupChunk(nextDate, 1L, nextDay)).thenReturn(result(nextDate, 1L, nextDay));
        double uploadedBefore = uploadedArticles();

        JobExecution completed = jobLauncher.run(job, parameters(firstDate));
        assertThat(completed.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(uploadedArticles()).isEqualTo(uploadedBefore + first.size() + last.size());
        JobExecution nextDayExecution = jobLauncher.run(job, parameters(nextDate));

        assertThat(nextDayExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(nextDayExecution.getJobInstance().getInstanceId())
                .isNotEqualTo(completed.getJobInstance().getInstanceId());
        assertThat(uploadedArticles())
                .isEqualTo(uploadedBefore + first.size() + last.size() + nextDay.size());
        verify(useCase).readArticles(nextDate, null, LIMIT);
        verify(useCase).backupChunk(nextDate, 1L, nextDay);
        verify(useCase, never()).backupChunk(eq(nextDate), eq(2L), any());
    }

    private JobParameters parameters(LocalDate targetDate) {
        return new JobParametersBuilder().addLocalDate("targetDate", targetDate).toJobParameters();
    }

    private List<ArticleBackupResult> result(
            LocalDate targetDate, long partNumber, List<ArticleResult> articles) {
        return List.of(
                new ArticleBackupResult(
                        "articles/" + targetDate + "/" + partNumber, targetDate, articles.size()));
    }

    private double uploadedArticles() {
        return registry.counter(ArticleBackupMetrics.UPLOADED_ARTICLES).count();
    }

    private ArticleResult article() {
        UUID id = UUID.randomUUID();
        return new ArticleResult(
                id,
                ArticleSourceType.NAVER,
                "https://example.com/" + id,
                "기사 제목",
                LocalDateTime.of(2000, 1, 2, 9, 0),
                Instant.parse("2000-01-02T01:00:00Z"),
                "기사 요약",
                0,
                0,
                false);
    }
}
