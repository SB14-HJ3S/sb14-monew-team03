package com.sprint.monew.adapter.in.batch.article.collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sprint.monew.application.article.provided.usecase.ArticleCollectionUseCase;
import com.sprint.monew.application.article.provided.usecase.dto.CollectedArticle;
import com.sprint.monew.application.interest.provided.usecase.dto.InterestKeywordResult;
import com.sprint.monew.domain.interest.entity.Interest;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.ExternalErrorType;
import com.sprint.monew.global.type.ArticleSourceType;
import com.sprint.monew.support.IntegrationTest;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

@IntegrationTest
class ArticleCollectionMetricsIntegrationTest {

    @MockitoBean private ArticleCollectionUseCase useCase;
    @Autowired private JobLauncher jobLauncher;
    @Autowired private MeterRegistry registry;
    @Autowired private JobExplorer jobExplorer;

    @Autowired
    @Qualifier("articleCollectionJob")
    private Job job;

    @ParameterizedTest
    @EnumSource(ArticleSourceType.class)
    @DisplayName("fetch 실패 후 나머지 저장을 마치고 Job을 FAILED로 처리하며 다음 정상 실행은 성공한다")
    void failJobAfterCompletingCollectionWhenFetchFails(ArticleSourceType source) throws Exception {
        Interest first = Interest.create("기술", List.of("AI"));
        Interest second = Interest.create("경제", List.of("경제"));
        givenTargets(first, second);
        var failure = new MonewException(ExternalErrorType.EXTERNAL_API_CALL_FAILED);
        if (source == ArticleSourceType.NAVER) {
            when(useCase.collectNaverArticles("AI")).thenThrow(failure).thenReturn(List.of());
        } else {
            when(useCase.collectRssArticles(source)).thenThrow(failure).thenReturn(List.of());
        }
        when(useCase.saveArticles(first, List.of())).thenReturn(3);
        when(useCase.saveArticles(second, List.of())).thenReturn(2);
        double previousSaved = savedArticles();
        double previousFailed = fetchFailures(source);

        JobExecution execution = runJob();

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(execution.getExitStatus().getExitCode())
                .isEqualTo(ExitStatus.FAILED.getExitCode());
        assertThat(savedArticles()).isEqualTo(previousSaved + 5);
        assertThat(fetchFailures(source)).isEqualTo(previousFailed + 1);
        var stored = jobExplorer.getJobExecution(execution.getId());
        assertThat(stored.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(stored.getStepExecutions())
                .singleElement()
                .satisfies(
                        step -> {
                            assertThat(step.getStatus()).isEqualTo(BatchStatus.FAILED);
                            assertThat(step.getExitStatus().getExitCode())
                                    .isEqualTo(ExitStatus.FAILED.getExitCode());
                        });

        JobExecution nextExecution = runJob();

        assertThat(nextExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(savedArticles()).isEqualTo(previousSaved + 10);
        assertThat(fetchFailures(source)).isEqualTo(previousFailed + 1);
        verify(useCase, times(2)).collectNaverArticles("경제");
        verify(useCase, times(2)).saveArticles(second, List.of());
    }

    @Test
    @DisplayName("각 chunk의 커밋 이후 해당 chunk에서 신규 저장한 기사 수만 증가한다")
    void countEachCommittedChunk() throws Exception {
        Interest first = Interest.create("기술", List.of("AI"));
        Interest second = Interest.create("경제", List.of("경제"));
        givenTargets(first, second);
        double previous = savedArticles();
        when(useCase.saveArticles(first, List.of()))
                .thenAnswer(
                        invocation -> {
                            assertThat(savedArticles()).isEqualTo(previous);
                            return 3;
                        });
        when(useCase.saveArticles(second, List.of()))
                .thenAnswer(
                        invocation -> {
                            assertThat(savedArticles()).isEqualTo(previous + 3);
                            return 2;
                        });

        JobExecution execution = runJob();

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(savedArticles()).isEqualTo(previous + 5);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    @DisplayName("입력 기사 수와 무관하게 실제 신규 저장 건수만 집계한다")
    void countOnlyNewArticles(int savedCount) throws Exception {
        Interest interest = Interest.create("기술", List.of("AI"));
        givenTargets(interest);
        List<CollectedArticle> articles =
                List.of(
                        new CollectedArticle(
                                ArticleSourceType.NAVER,
                                "https://example.com/first",
                                "AI 뉴스 1",
                                LocalDateTime.of(2026, 11, 3, 12, 0),
                                "AI 뉴스 요약"),
                        new CollectedArticle(
                                ArticleSourceType.NAVER,
                                "https://example.com/second",
                                "AI 뉴스 2",
                                LocalDateTime.of(2026, 11, 3, 12, 0),
                                "AI 뉴스 요약"));
        when(useCase.collectNaverArticles("AI")).thenReturn(articles);
        when(useCase.saveArticles(interest, articles)).thenReturn(savedCount);
        double previous = savedArticles();

        JobExecution execution = runJob();

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(savedArticles()).isEqualTo(previous + savedCount);
        verify(useCase).saveArticles(interest, articles);
    }

    @Test
    @DisplayName("다음 chunk 저장 실패로 Job이 실패해도 앞서 커밋한 건수는 유지한다")
    void retainCommittedCountWhenNextWriteFails() throws Exception {
        Interest first = Interest.create("기술", List.of("AI"));
        Interest second = Interest.create("경제", List.of("경제"));
        givenTargets(first, second);
        when(useCase.saveArticles(first, List.of())).thenReturn(3);
        when(useCase.saveArticles(second, List.of())).thenThrow(new IllegalStateException("저장 실패"));
        double previous = savedArticles();

        JobExecution execution = runJob();

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(savedArticles()).isEqualTo(previous + 3);
    }

    @Test
    @DisplayName("Writer가 반환한 뒤 커밋에서 롤백된 chunk는 집계하지 않는다")
    void excludeChunkWhenCommitFails() throws Exception {
        Interest first = Interest.create("기술", List.of("AI"));
        Interest second = Interest.create("경제", List.of("경제"));
        givenTargets(first, second);
        when(useCase.saveArticles(first, List.of())).thenReturn(3);
        when(useCase.saveArticles(second, List.of()))
                .thenAnswer(
                        invocation -> {
                            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                            return 2;
                        });
        double previous = savedArticles();

        JobExecution execution = runJob();

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(savedArticles()).isEqualTo(previous + 3);
    }

    @Test
    @DisplayName("처리할 관심사가 없으면 저장 기사 수는 증가하지 않는다")
    void doNotCountEmptyJob() throws Exception {
        givenTargets();
        double previous = savedArticles();

        JobExecution execution = runJob();

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(savedArticles()).isEqualTo(previous);
    }

    private void givenTargets(Interest... interests) {
        when(useCase.findValidInterestKeywords())
                .thenReturn(
                        java.util.Arrays.stream(interests)
                                .map(InterestKeywordResult::from)
                                .toList());
    }

    private JobExecution runJob() throws Exception {
        return jobLauncher.run(
                job,
                new JobParametersBuilder()
                        .addString("testRun", UUID.randomUUID().toString())
                        .toJobParameters());
    }

    private double savedArticles() {
        return registry.counter(ArticleCollectionMetrics.SAVED_ARTICLES).count();
    }

    private double fetchFailures(ArticleSourceType source) {
        return registry.counter(ArticleCollectionMetrics.FETCH_FAILED, "source", source.name())
                .count();
    }
}
