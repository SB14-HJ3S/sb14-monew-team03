package com.sprint.monew.adapter.in.scheduler;

import com.sprint.monew.adapter.in.batch.ArticleCollectionRunner;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "monew.scheduling.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class ArticleCollectionScheduler {

    private final ArticleCollectionRunner runner;

    @Scheduled(cron = "${monew.news.collection.cron}", zone = "Asia/Seoul")
    public void collect() {
        runner.runCollection();
    }
}
