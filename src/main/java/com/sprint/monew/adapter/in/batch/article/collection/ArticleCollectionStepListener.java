package com.sprint.monew.adapter.in.batch.article.collection;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.stereotype.Component;

@Slf4j
@StepScope
@Component
@RequiredArgsConstructor
public class ArticleCollectionStepListener implements StepExecutionListener {

    private final MeterRegistry meterRegistry;
    private final ArticleCollectionTiming timing;
    private double initialFailureCount;

    private long startedNanos;

    @Override
    public void beforeStep(StepExecution stepExecution) {
        this.initialFailureCount = fetchFailureCount();
        this.startedNanos = System.nanoTime();
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        double elapsedSeconds = (System.nanoTime() - startedNanos) / 1_000_000_000.0;

        log.info("기사 수집 Step 소요시간: {}초, 처리 관심사={}개", elapsedSeconds, stepExecution.getReadCount());
        log.info(
                "기사 수집 시간 요약: 외부 수집={}초 (네이버={}초, RSS={}초), DB 적재={}초"
                        + " (Writer 시작~chunk 커밋/롤백 완료, 실패 시도 포함)",
                seconds(timing.naverSeconds() + timing.rssSeconds()),
                seconds(timing.naverSeconds()),
                seconds(timing.rssSeconds()),
                seconds(timing.storageSeconds()));

        if (stepExecution.getStatus() != BatchStatus.COMPLETED) {
            return null;
        }
        double failureCount = fetchFailureCount() - initialFailureCount;
        if (failureCount <= 0) {
            return null;
        }

        stepExecution.setStatus(BatchStatus.FAILED);
        return ExitStatus.FAILED;
    }

    private double fetchFailureCount() {
        return meterRegistry.find(ArticleCollectionMetrics.FETCH_FAILED).counters().stream()
                .mapToDouble(Counter::count)
                .sum();
    }

    private String seconds(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }
}
