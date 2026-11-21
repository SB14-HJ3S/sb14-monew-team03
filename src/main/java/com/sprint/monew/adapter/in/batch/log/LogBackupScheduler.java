package com.sprint.monew.adapter.in.batch.log;

import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
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
@Slf4j
@ConditionalOnProperty(
        name = "monew.scheduling.enabled",
        havingValue = "true",
        matchIfMissing = true)
@RequiredArgsConstructor
public class LogBackupScheduler {

    private final JobLauncher jobLauncher;
    private final Job logBackupJob;

    @Scheduled(cron = "${monew.log.backup.cron}", zone = "Asia/Seoul")
    public void backup()
            throws JobInstanceAlreadyCompleteException,
                    JobExecutionAlreadyRunningException,
                    JobParametersInvalidException,
                    JobRestartException {
        LocalDate targetDate = LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(1);
        JobParameters parameters =
                new JobParametersBuilder()
                        .addLocalDate("logTargetDate", targetDate)
                        .toJobParameters();
        jobLauncher.run(logBackupJob, parameters);
    }
}
