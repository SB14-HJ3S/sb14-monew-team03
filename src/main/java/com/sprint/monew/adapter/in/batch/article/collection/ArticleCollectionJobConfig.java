package com.sprint.monew.adapter.in.batch.article.collection;

import com.sprint.monew.adapter.in.batch.article.collection.dto.InterestCollectedArticles;
import com.sprint.monew.application.interest.provided.usecase.dto.InterestKeywordResult;
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
public class ArticleCollectionJobConfig {

    @Bean
    public Step articleCollectionStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ArticleCollectReader reader,
            ArticleCollectProcessor processor,
            ArticleCollectWriter writer,
            ArticleCollectionListener chunkListener,
            ArticleCollectionStepListener stepListener) {
        return new StepBuilder("articleCollectionStep", jobRepository)
                .<InterestKeywordResult, InterestCollectedArticles>chunk(1, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .listener((ChunkListener) chunkListener)
                .listener(stepListener)
                .build();
    }

    @Bean
    public Job articleCollectionJob(JobRepository jobRepository, Step articleCollectionStep) {
        return new JobBuilder("articleCollectionJob", jobRepository)
                .start(articleCollectionStep)
                .build();
    }
}
