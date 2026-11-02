package com.sprint.monew.adapter.in.batch.article.collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.sprint.monew.application.article.provided.ArticleCollectionUseCase;
import com.sprint.monew.application.interest.provided.dto.InterestKeywordResult;
import com.sprint.monew.domain.interest.entity.Interest;
import com.sprint.monew.support.IntegrationTest;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
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

    @Autowired
    @Qualifier("articleCollectionJob")
    private Job job;

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
}
