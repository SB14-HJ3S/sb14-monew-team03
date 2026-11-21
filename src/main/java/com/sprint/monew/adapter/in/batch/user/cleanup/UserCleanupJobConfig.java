package com.sprint.monew.adapter.in.batch.user.cleanup;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserCleanupJobConfig {

    @Bean
    public Step userCleanupStep(
            JobRepository jobRepository,
            UserCleanupTasklet tasklet,
            UserCleanupStepListener listener) {
        return new StepBuilder("userCleanupStep", jobRepository)
                .tasklet(tasklet, new ResourcelessTransactionManager())
                .listener(listener)
                .build();
    }

    @Bean
    public Job userCleanupJob(JobRepository jobRepository, Step userCleanupStep) {
        return new JobBuilder("userCleanupJob", jobRepository).start(userCleanupStep).build();
    }
}
