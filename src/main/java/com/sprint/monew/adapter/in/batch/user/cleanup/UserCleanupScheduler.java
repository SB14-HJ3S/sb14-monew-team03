package com.sprint.monew.adapter.in.batch.user.cleanup;

import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "monew.scheduling.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class UserCleanupScheduler {

    private final JobLauncher jobLauncher;
    private final Job userCleanupJob;

    @Scheduled(cron = "${monew.user.cleanup.cron}", zone = "Asia/Seoul")
    public void cleanup()
            throws JobInstanceAlreadyCompleteException,
                    JobExecutionAlreadyRunningException,
                    JobParametersInvalidException,
                    JobRestartException {
        JobParameters parameters =
                new JobParametersBuilder()
                        .addLocalDateTime("dateTime", LocalDateTime.now(ZoneId.of("Asia/Seoul")))
                        .toJobParameters();

        jobLauncher.run(userCleanupJob, parameters);
    }
}
