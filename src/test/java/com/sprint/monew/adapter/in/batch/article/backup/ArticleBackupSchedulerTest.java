package com.sprint.monew.adapter.in.batch.article.backup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

@ExtendWith(MockitoExtension.class)
class ArticleBackupSchedulerTest {

    @Mock private JobLauncher jobLauncher;
    @Mock private Job articleBackupJob;

    @Test
    @DisplayName("한국 시간 기준 전날을 식별 파라미터로 전달한다")
    void launchBackupForPreviousDayInSeoul() throws Exception {
        LocalDate today = LocalDate.of(2026, 1, 1);
        LocalDate yesterday = today.minusDays(1);
        ArticleBackupScheduler scheduler =
                new ArticleBackupScheduler(jobLauncher, articleBackupJob);

        try (MockedStatic<LocalDate> clock = mockStatic(LocalDate.class, CALLS_REAL_METHODS)) {
            clock.when(() -> LocalDate.now(ZoneId.of("Asia/Seoul"))).thenReturn(today);

            scheduler.backup();
        }

        ArgumentCaptor<JobParameters> captor = ArgumentCaptor.forClass(JobParameters.class);
        verify(jobLauncher).run(eq(articleBackupJob), captor.capture());
        assertThat(captor.getValue().getLocalDate("targetDate")).isEqualTo(yesterday);
        assertThat(captor.getValue().getParameter("targetDate").isIdentifying()).isTrue();
    }

    @Test
    @DisplayName("JobLauncher의 실행 결과를 반환한다")
    void returnJobExecution() throws Exception {
        JobExecution expected = new JobExecution(1L);
        when(jobLauncher.run(eq(articleBackupJob), any(JobParameters.class))).thenReturn(expected);
        ArticleBackupScheduler scheduler =
                new ArticleBackupScheduler(jobLauncher, articleBackupJob);

        assertThat(scheduler.backup()).isSameAs(expected);
    }

    @Test
    @DisplayName("이미 완료된 Job의 재실행 예외를 호출자에게 전달한다")
    void propagateAlreadyCompletedFailure() throws Exception {
        JobInstanceAlreadyCompleteException failure =
                new JobInstanceAlreadyCompleteException("이미 완료된 날짜");
        when(jobLauncher.run(eq(articleBackupJob), any(JobParameters.class))).thenThrow(failure);
        ArticleBackupScheduler scheduler =
                new ArticleBackupScheduler(jobLauncher, articleBackupJob);

        assertThatThrownBy(scheduler::backup).isSameAs(failure);
    }

    @Test
    @DisplayName("스케줄링이 비활성화되면 Scheduler를 등록하지 않는다")
    void disableSchedulerByProperty() {
        contextRunner()
                .withPropertyValues("monew.scheduling.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(ArticleBackupScheduler.class));
    }

    @Test
    @DisplayName("스케줄링 활성화 설정이 없으면 Scheduler를 등록한다")
    void enableSchedulerByDefault() {
        contextRunner()
                .run(context -> assertThat(context).hasSingleBean(ArticleBackupScheduler.class));
    }

    private ApplicationContextRunner contextRunner() {
        return new ApplicationContextRunner()
                .withUserConfiguration(ArticleBackupScheduler.class)
                .withBean(JobLauncher.class, () -> jobLauncher)
                .withBean("articleBackupJob", Job.class, () -> articleBackupJob);
    }
}
