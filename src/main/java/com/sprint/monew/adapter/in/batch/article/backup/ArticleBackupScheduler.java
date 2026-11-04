package com.sprint.monew.adapter.in.batch.article.backup;

import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "monew.scheduling.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class ArticleBackupScheduler {

    private final JobLauncher jobLauncher;
    private final Job articleBackupJob;

    @Scheduled(cron = "${monew.article.backup.cron}")
    public JobExecution backup()
            throws JobInstanceAlreadyCompleteException,
                    JobExecutionAlreadyRunningException,
                    JobParametersInvalidException,
                    JobRestartException {
        LocalDate targetDate = LocalDate.now(ZoneId.systemDefault()).minusDays(1L);
        JobParameters parameters =
                new JobParametersBuilder().addLocalDate("targetDate", targetDate).toJobParameters();

        return jobLauncher.run(articleBackupJob, parameters);
    }
}
