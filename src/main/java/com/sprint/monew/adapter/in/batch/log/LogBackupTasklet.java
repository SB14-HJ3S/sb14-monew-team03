package com.sprint.monew.adapter.in.batch.log;

import com.sprint.monew.application.log.provided.LogBackupUseCase;
import com.sprint.monew.application.log.provided.dto.LogBackupResult;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LogBackupTasklet implements Tasklet {

    private final LogBackupUseCase logBackupUseCase;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext)
            throws Exception {
        LocalDate targetDate =
                contribution.getStepExecution().getJobParameters().getLocalDate("logTargetDate");

        LogBackupResult result = logBackupUseCase.backup(targetDate);

        StepExecution stepExecution = contribution.getStepExecution();
        ExecutionContext executionContext = stepExecution.getExecutionContext();
        executionContext.putInt(LogMetrics.TOTAL_FILES, result.totalFileCount());
        executionContext.putInt(LogMetrics.UPLOADED_FILES, result.uploadedFileCount());

        if (result.uploadedFileCount() < result.totalFileCount()) {
            throw new LogBackupFailedException(
                    targetDate, result.totalFileCount(), result.uploadedFileCount());
        }

        return RepeatStatus.FINISHED;
    }
}
