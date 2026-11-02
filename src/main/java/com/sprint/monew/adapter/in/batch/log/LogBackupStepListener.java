package com.sprint.monew.adapter.in.batch.log;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.stereotype.Component;

@Component
public class LogBackupStepListener implements StepExecutionListener {

    private final Counter totalFilesCounter;
    private final Counter uploadedFilesCounter;

    public LogBackupStepListener(MeterRegistry meterRegistry) {
        this.totalFilesCounter = meterRegistry.counter(LogMetrics.TOTAL_FILES);
        this.uploadedFilesCounter = meterRegistry.counter(LogMetrics.UPLOADED_FILES);
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        ExecutionContext context = stepExecution.getExecutionContext();
        int totalFileCount = context.getInt(LogMetrics.TOTAL_FILES, 0);
        int uploadedFileCount = context.getInt(LogMetrics.UPLOADED_FILES, 0);

        totalFilesCounter.increment(totalFileCount);
        uploadedFilesCounter.increment(uploadedFileCount);
        return null;
    }
}
