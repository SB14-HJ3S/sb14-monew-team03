package com.sprint.monew.adapter.in.batch.log;

import java.nio.file.Path;
import java.util.List;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class LogBackupJobConfig {

    @Bean
    public Step logBackupStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            LogBackupReader reader,
            LogBackupWriter writer,
            LogBackupListener listener) {
        return new StepBuilder("logBackupStep", jobRepository)
                .<List<Path>, List<Path>>chunk(1, transactionManager)
                .reader(reader)
                .writer(writer)
                .listener((ChunkListener) listener)
                .build();
    }

    @Bean
    public Job logBackupJob(JobRepository jobRepository, Step logBackupStep) {
        return new JobBuilder("logBackupJob", jobRepository).start(logBackupStep).build();
    }
}
