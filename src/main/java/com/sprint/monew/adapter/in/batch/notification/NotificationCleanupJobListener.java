package com.sprint.monew.adapter.in.batch.notification;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationCleanupJobListener implements JobExecutionListener {

    private final Counter deletedCounter;

    public NotificationCleanupJobListener(MeterRegistry meterRegistry) {
        this.deletedCounter = meterRegistry.counter(NotificationMetrics.DELETED);
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        if (jobExecution.getStatus() != BatchStatus.COMPLETED) {
            return;
        }

        int deletedCount =
                jobExecution.getStepExecutions().stream()
                        .mapToInt(
                                step ->
                                        step.getExecutionContext()
                                                .getInt(NotificationMetrics.DELETED, 0))
                        .sum();

        deletedCounter.increment(deletedCount);
    }
}
