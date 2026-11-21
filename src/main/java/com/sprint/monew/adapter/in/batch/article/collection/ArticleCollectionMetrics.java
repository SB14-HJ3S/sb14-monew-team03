package com.sprint.monew.adapter.in.batch.article.collection;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ArticleCollectionMetrics {

    static final String FETCH_SUCCESS = "batch.article.collection.fetch.success";
    static final String FETCH_FAILED = "batch.article.collection.fetch.failed";
    static final String FETCHED_ARTICLES = "batch.article.collection.fetched.articles";
    static final String SAVED_ARTICLES = "batch.article.collection.saved";
}
