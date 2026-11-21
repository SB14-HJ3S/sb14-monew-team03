package com.sprint.monew.adapter.in.batch.article.collection;

import com.sprint.monew.global.type.ArticleSourceType;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.stereotype.Component;

/** 단일 스레드 수집 Step의 실행별 누적 시간. 실패한 호출과 저장 시도도 포함한다. */
@Component
@StepScope
public class ArticleCollectionTiming {

    private final Map<ArticleSourceType, Long> fetchNanos = new EnumMap<>(ArticleSourceType.class);
    private Long storageStartedNanos;
    private long storageNanos;

    public <T> T measureFetch(ArticleSourceType source, Supplier<T> fetch) {
        long started = System.nanoTime();
        try {
            return fetch.get();
        } finally {
            fetchNanos.merge(source, System.nanoTime() - started, Long::sum);
        }
    }

    public void startStorage() {
        if (storageStartedNanos == null) {
            storageStartedNanos = System.nanoTime();
        }
    }

    // Writer 반환 뒤의 JPA flush와 chunk 커밋/롤백이 끝난 후 호출한다.
    public void finishStorage() {
        if (storageStartedNanos != null) {
            storageNanos += System.nanoTime() - storageStartedNanos;
            storageStartedNanos = null;
        }
    }

    public double naverSeconds() {
        return fetchNanos.getOrDefault(ArticleSourceType.NAVER, 0L) / 1_000_000_000.0;
    }

    public double rssSeconds() {
        return fetchNanos.entrySet().stream()
                        .filter(entry -> entry.getKey() != ArticleSourceType.NAVER)
                        .mapToLong(Map.Entry::getValue)
                        .sum()
                / 1_000_000_000.0;
    }

    public double storageSeconds() {
        return storageNanos / 1_000_000_000.0;
    }
}
