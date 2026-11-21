package com.sprint.monew.adapter.in.batch.article.backup;

import com.sprint.monew.application.article.provided.usecase.dto.ArticleResult;
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
public class ArticleBackupJobConfig {

    @Bean
    public Step articleBackupStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ArticleBackupReader articleBackupReader,
            ArticleBackupWriter articleBackupWriter,
            ArticleBackupListener listener) {
        return new StepBuilder("articleBackupStep", jobRepository)
                .<List<ArticleResult>, List<ArticleResult>>chunk(1, transactionManager)
                .reader(articleBackupReader)
                .writer(articleBackupWriter)
                .listener((ChunkListener) listener)
                .build();
    }

    @Bean
    public Job articleBackupJob(JobRepository jobRepository, Step articleBackupStep) {
        return new JobBuilder("articleBackupJob", jobRepository).start(articleBackupStep).build();
    }
}
