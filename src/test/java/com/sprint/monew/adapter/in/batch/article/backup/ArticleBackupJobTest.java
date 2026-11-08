package com.sprint.monew.adapter.in.batch.article.backup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sprint.monew.application.article.provided.usecase.ArticleBackupUseCase;
import com.sprint.monew.application.article.provided.usecase.dto.ArticleBackupResult;
import com.sprint.monew.application.article.provided.usecase.dto.ArticleResult;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.ArticleErrorType;
import com.sprint.monew.global.type.ArticleSourceType;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.ResourcelessJobRepository;
import org.springframework.batch.core.scope.StepScope;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;

class ArticleBackupJobTest {

    private static final int LIMIT = 2;
    private static final LocalDate TARGET_DATE = LocalDate.of(2000, 1, 2);

    private final ArticleBackupUseCase useCase = mock(ArticleBackupUseCase.class);
    private AnnotationConfigApplicationContext context;
    private ArticleBackupReader reader;
    private ArticleBackupWriter writer;

    @BeforeEach
    void setUp() {
        // 실제 StepScope와 Job 구성을 사용하되 DB와 S3는 호출하지 않는다.
        context = new AnnotationConfigApplicationContext();
        TestPropertyValues.of("monew.article.limit=" + LIMIT).applyTo(context);
        context.registerBean("stepScope", StepScope.class);
        context.registerBean(ArticleBackupUseCase.class, () -> useCase);
        context.registerBean(MeterRegistry.class, SimpleMeterRegistry::new);
        context.registerBean(JobRepository.class, ResourcelessJobRepository::new);
        context.registerBean(PlatformTransactionManager.class, ResourcelessTransactionManager::new);
        context.register(
                ArticleBackupJobConfig.class,
                ArticleBackupReader.class,
                ArticleBackupWriter.class,
                ArticleBackupListener.class);
        context.refresh();
        reader = context.getBean(ArticleBackupReader.class);
        writer = context.getBean(ArticleBackupWriter.class);
    }

    @AfterEach
    void tearDown() {
        StepSynchronizationManager.close();
        context.close();
    }

    @Test
    @DisplayName("첫 조회는 커서 없이 시작하고 짧은 페이지도 마지막 기사 ID로 다음 조회를 이어간다")
    void paginateUsingLastArticleId() throws Exception {
        List<ArticleResult> first = List.of(article(), article());
        List<ArticleResult> second = List.of(article());
        List<ArticleResult> third = List.of(article());
        when(useCase.readArticles(any(LocalDate.class), isNull(), eq(LIMIT))).thenReturn(first);
        when(useCase.readArticles(any(LocalDate.class), eq(first.get(1).id()), eq(LIMIT)))
                .thenReturn(second);
        when(useCase.readArticles(any(LocalDate.class), eq(second.get(0).id()), eq(LIMIT)))
                .thenReturn(third);
        openStep(TARGET_DATE);

        assertThat(reader.read()).containsExactlyElementsOf(first);
        assertThat(reader.read()).containsExactlyElementsOf(second);
        assertThat(reader.read()).containsExactlyElementsOf(third);
    }

    @Test
    @DisplayName("백업 대상이 처음부터 없으면 null을 반환하여 Step을 종료한다")
    void returnNullWhenNoArticlesExist() throws Exception {
        when(useCase.readArticles(any(LocalDate.class), isNull(), eq(LIMIT))).thenReturn(List.of());
        openStep(TARGET_DATE);

        assertThat(reader.read()).isNull();
    }

    @Test
    @DisplayName("마지막 페이지를 읽은 다음 빈 결과를 만나면 null을 반환한다")
    void returnNullAfterLastPage() throws Exception {
        List<ArticleResult> page = List.of(article());
        when(useCase.readArticles(any(LocalDate.class), isNull(), eq(LIMIT))).thenReturn(page);
        when(useCase.readArticles(any(LocalDate.class), eq(page.get(0).id()), eq(LIMIT)))
                .thenReturn(List.of());
        openStep(TARGET_DATE);

        assertThat(reader.read()).containsExactlyElementsOf(page);
        assertThat(reader.read()).isNull();
    }

    @Test
    @DisplayName("Reader는 실행 시점과 무관하게 JobParameter의 날짜로 조회한다")
    void readArticlesForTargetDate() throws Exception {
        when(useCase.readArticles(any(LocalDate.class), isNull(), eq(LIMIT)))
                .thenReturn(List.of(article()));
        openStep(TARGET_DATE);

        reader.read();

        verify(useCase).readArticles(TARGET_DATE, null, LIMIT);
    }

    @Test
    @DisplayName("새 Step 실행에서는 이전 조회 커서를 초기화한다")
    void resetCursorForNewStep() throws Exception {
        List<ArticleResult> page = List.of(article());
        when(useCase.readArticles(any(LocalDate.class), isNull(), eq(LIMIT))).thenReturn(page);
        openStep(TARGET_DATE);
        assertThat(reader.read()).containsExactlyElementsOf(page);
        StepSynchronizationManager.close();
        openStep(TARGET_DATE.plusDays(1));

        assertThat(reader.read()).containsExactlyElementsOf(page);
    }

    @Test
    @DisplayName("조회 예외는 배치에 그대로 전달한다")
    void propagateReadFailure() {
        RuntimeException failure = new IllegalStateException("기사 조회 실패");
        when(useCase.readArticles(any(LocalDate.class), isNull(), eq(LIMIT))).thenThrow(failure);
        openStep(TARGET_DATE);

        assertThatThrownBy(() -> reader.read()).isSameAs(failure);
    }

    @Test
    @DisplayName("Writer는 실행 시점과 무관하게 JobParameter의 날짜로 백업한다")
    void writeArticlesForTargetDate() throws Exception {
        List<ArticleResult> page = List.of(article());
        openStep(TARGET_DATE);

        writer.write(new Chunk<>(List.of(page)));

        verify(useCase).backupChunk(eq(TARGET_DATE), anyLong(), eq(page));
    }

    @Test
    @DisplayName("Reader를 다시 열면 저장된 커서와 이전 실행의 페이지 크기를 복원한다")
    void restoreReaderCheckpoint() throws Exception {
        UUID afterId = UUID.randomUUID();
        ExecutionContext checkpoint = new ExecutionContext();
        checkpoint.putString(ArticleBackupReader.AFTER_ID, afterId.toString());
        checkpoint.putInt(ArticleBackupReader.LIMIT, LIMIT + 1);
        when(useCase.readArticles(TARGET_DATE, afterId, LIMIT + 1)).thenReturn(List.of());
        openStep(TARGET_DATE);

        reader.open(checkpoint);
        assertThat(reader.read()).isNull();

        verify(useCase).readArticles(TARGET_DATE, afterId, LIMIT + 1);
    }

    @Test
    @DisplayName("Writer를 다시 열면 저장된 파일 번호로 업로드한다")
    void restoreWriterCheckpoint() throws Exception {
        List<ArticleResult> page = List.of(article());
        ExecutionContext checkpoint = new ExecutionContext();
        checkpoint.putLong(ArticleBackupWriter.NEXT_PART_NUMBER, 4L);
        openStep(TARGET_DATE);

        writer.open(checkpoint);
        writer.write(new Chunk<>(List.of(page)));
        writer.update(checkpoint);

        verify(useCase).backupChunk(TARGET_DATE, 4L, page);
        assertThat(checkpoint.getLong(ArticleBackupWriter.NEXT_PART_NUMBER)).isEqualTo(5L);
    }

    @Test
    @DisplayName("업로드 실패를 다시 던지고 같은 청크를 재시도할 때 파일 번호를 유지한다")
    void retainPartNumberAfterUploadFailure() throws Exception {
        List<ArticleResult> page = List.of(article());
        MonewException failure = new MonewException(ArticleErrorType.ARTICLE_BACKUP_UPLOAD_FAILED);
        when(useCase.backupChunk(TARGET_DATE, 1L, page))
                .thenThrow(failure)
                .thenReturn(
                        List.of(new ArticleBackupResult("backup-key", TARGET_DATE, page.size())));
        openStep(TARGET_DATE);

        assertThatThrownBy(() -> writer.write(new Chunk<>(List.of(page)))).isSameAs(failure);
        writer.write(new Chunk<>(List.of(page)));

        verify(useCase, times(2)).backupChunk(TARGET_DATE, 1L, page);
    }

    @Test
    @DisplayName("백업 파일 번호는 1부터 시작하고 묶음마다 순서대로 증가한다")
    void numberBackupPartsFromOne() throws Exception {
        List<ArticleResult> first = List.of(article(), article());
        List<ArticleResult> second = List.of(article());
        List<ArticleResult> third = List.of(article());
        openStep(TARGET_DATE);

        writer.write(new Chunk<>(List.of(first, second)));
        writer.write(new Chunk<>(List.of(third)));

        var order = inOrder(useCase);
        order.verify(useCase).backupChunk(any(LocalDate.class), eq(1L), eq(first));
        order.verify(useCase).backupChunk(any(LocalDate.class), eq(2L), eq(second));
        order.verify(useCase).backupChunk(any(LocalDate.class), eq(3L), eq(third));
    }

    @Test
    @DisplayName("새 Step 실행에서는 백업 파일 번호를 초기값으로 되돌린다")
    void resetPartNumberForNewStep() throws Exception {
        List<ArticleResult> first = List.of(article());
        List<ArticleResult> second = List.of(article());
        openStep(TARGET_DATE);
        writer.write(new Chunk<>(List.of(first, second)));
        StepSynchronizationManager.close();
        openStep(TARGET_DATE.plusDays(1));
        List<ArticleResult> nextDay = List.of(article());

        writer.write(new Chunk<>(List.of(nextDay)));

        ArgumentCaptor<Long> firstPart = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> nextDayPart = ArgumentCaptor.forClass(Long.class);
        verify(useCase).backupChunk(any(LocalDate.class), firstPart.capture(), eq(first));
        verify(useCase).backupChunk(any(LocalDate.class), nextDayPart.capture(), eq(nextDay));
        assertThat(nextDayPart.getValue()).isEqualTo(firstPart.getValue());
    }

    @Test
    @DisplayName("빈 Chunk는 업로드하지 않는다")
    void skipEmptyChunk() throws Exception {
        openStep(TARGET_DATE);

        writer.write(new Chunk<>());

        verifyNoInteractions(useCase);
    }

    @Test
    @DisplayName("업로드 실패를 그대로 전달하고 다음 묶음은 업로드하지 않는다")
    void stopWritingAfterUploadFailure() {
        List<ArticleResult> first = List.of(article());
        List<ArticleResult> second = List.of(article());
        RuntimeException failure = new IllegalStateException("S3 업로드 실패");
        when(useCase.backupChunk(any(LocalDate.class), anyLong(), eq(first))).thenThrow(failure);
        openStep(TARGET_DATE);

        assertThatThrownBy(() -> writer.write(new Chunk<>(List.of(first, second))))
                .isSameAs(failure);
        verify(useCase, never()).backupChunk(any(LocalDate.class), anyLong(), eq(second));
    }

    @Test
    @DisplayName("대상 기사가 없는 Job은 업로드 없이 COMPLETED로 종료한다")
    void completeJobWithoutArticles() throws Exception {
        when(useCase.readArticles(any(LocalDate.class), isNull(), eq(LIMIT))).thenReturn(List.of());

        JobExecution execution = launchJob();

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(execution.getStepExecutions())
                .singleElement()
                .satisfies(step -> assertThat(step.getWriteCount()).isZero());
        verify(useCase, never()).backupChunk(any(), anyLong(), any());
    }

    @Test
    @DisplayName("여러 페이지와 마지막 남은 기사를 모두 업로드한 Job은 COMPLETED로 종료한다")
    void completeJobAfterUploadingAllPages() throws Exception {
        List<ArticleResult> first = List.of(article(), article());
        List<ArticleResult> last = List.of(article());
        when(useCase.readArticles(any(LocalDate.class), isNull(), eq(LIMIT))).thenReturn(first);
        when(useCase.readArticles(any(LocalDate.class), eq(first.get(1).id()), eq(LIMIT)))
                .thenReturn(last);
        when(useCase.readArticles(any(LocalDate.class), eq(last.get(0).id()), eq(LIMIT)))
                .thenReturn(List.of());

        JobExecution execution = launchJob();

        var order = inOrder(useCase);
        order.verify(useCase).backupChunk(any(LocalDate.class), anyLong(), eq(first));
        order.verify(useCase).backupChunk(any(LocalDate.class), anyLong(), eq(last));
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(execution.getStepExecutions())
                .singleElement()
                .satisfies(step -> assertThat(step.getWriteCount()).isEqualTo(2));
    }

    @Test
    @DisplayName("업로드 예외가 발생하면 Job은 FAILED로 종료하고 원인을 보존한다")
    void failJobWhenUploadFails() throws Exception {
        List<ArticleResult> page = List.of(article());
        RuntimeException failure = new IllegalStateException("S3 업로드 실패");
        when(useCase.readArticles(any(LocalDate.class), isNull(), eq(LIMIT))).thenReturn(page);
        when(useCase.backupChunk(any(LocalDate.class), anyLong(), eq(page))).thenThrow(failure);

        JobExecution execution = launchJob();

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(execution.getAllFailureExceptions()).contains(failure);
    }

    private void openStep(LocalDate date) {
        JobExecution execution = new JobExecution(1L, parameters(date));
        StepSynchronizationManager.register(execution.createStepExecution("articleBackupStep"));
    }

    private JobExecution launchJob() throws Exception {
        TaskExecutorJobLauncher launcher = new TaskExecutorJobLauncher();
        launcher.setJobRepository(context.getBean(JobRepository.class));
        launcher.afterPropertiesSet();
        return launcher.run(
                context.getBean("articleBackupJob", Job.class), parameters(TARGET_DATE));
    }

    private JobParameters parameters(LocalDate date) {
        return new JobParametersBuilder().addLocalDate("targetDate", date).toJobParameters();
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
