package com.sprint.monew.application.article.provided.dto;

// 전체 수집결과
public record ArticleCollectionReport(
        SourceResult naver, SourceResult rss, int newArticleCount, int saveFailureCount) {
    public int totalCollectedCount() {
        return naver.collectedCount() + rss.collectedCount();
    }

    public int totalFailureCount() {
        return naver.failureCount() + rss.failureCount() + saveFailureCount;
    }

    public boolean hasPartialFailure() {
        return totalFailureCount() > 0;
    }
}
