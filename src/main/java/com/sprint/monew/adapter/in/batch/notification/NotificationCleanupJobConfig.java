package com.sprint.monew.adapter.in.batch.notification;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class NotificationCleanupJobConfig {

    @Bean
    public Step notificationCleanupStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            NotificationCleanupTasklet tasklet,
            NotificationCleanupStepListener listener) {
        return new StepBuilder("notificationCleanupStep", jobRepository)
                .tasklet(tasklet, transactionManager)
                .listener(listener)
                .build();
    }

    @Bean
    public Job notificationCleanupJob(JobRepository jobRepository, Step notificationCleanupStep) {
        return new JobBuilder("notificationCleanupJob", jobRepository)
                .start(notificationCleanupStep)
                .build();
    }
}
