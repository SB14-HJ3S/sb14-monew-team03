package com.sprint.monew.adapter.in.batch.user.cleanup;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component
public class UserCleanupStepListener implements StepExecutionListener {

    private final Counter deletedCounter;

    public UserCleanupStepListener(MeterRegistry meterRegistry) {
        this.deletedCounter = meterRegistry.counter(UserCleanupMetrics.DELETED);
    }

    @Nullable
    public ExitStatus afterStep(StepExecution stepExecution) {
        if (stepExecution.getStatus() != BatchStatus.COMPLETED) {
            return null;
        }

        long deletedCount = stepExecution.getWriteCount();
        deletedCounter.increment(deletedCount);
        return ExitStatus.COMPLETED;
    }
}
