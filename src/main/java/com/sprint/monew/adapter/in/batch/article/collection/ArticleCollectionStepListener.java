package com.sprint.monew.adapter.in.batch.article.collection;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.stereotype.Component;

@StepScope
@Component
@RequiredArgsConstructor
public class ArticleCollectionStepListener implements StepExecutionListener {

    private final MeterRegistry meterRegistry;
    private double initialFailureCount;

    @Override
    public void beforeStep(StepExecution stepExecution) {
        this.initialFailureCount = fetchFailureCount();
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
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
}
