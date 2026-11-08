package com.sprint.monew.adapter.in.batch.log;

import com.sprint.monew.application.log.provided.usecase.LogBackupUseCase;
import com.sprint.monew.application.log.provided.usecase.dto.LogBackupResult;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@StepScope
@Component
@RequiredArgsConstructor
public class LogBackupWriter implements ItemStreamWriter<List<Path>> {

    private final LogBackupUseCase useCase;

    @Value("#{jobParameters['logTargetDate']}")
    private LocalDate targetDate;

    private long partNumber = 1L;

    @Override
    public void write(Chunk<? extends List<Path>> chunk) {
        for (List<Path> files : chunk) {
            LogBackupResult result = useCase.backup(targetDate, partNumber, files);
            if (result.uploadedFileCount() < result.totalFileCount()) {
                throw new LogBackupFailedException(
                        targetDate, result.totalFileCount(), result.uploadedFileCount());
            }
            partNumber++;
        }
    }

    static final String NEXT_PART_NUMBER = "logBackupWriter.nextPartNumber";

    @Override
    public void open(ExecutionContext executionContext) {
        partNumber = executionContext.getLong(NEXT_PART_NUMBER, 1L);
    }

    @Override
    public void update(ExecutionContext executionContext) {
        executionContext.putLong(NEXT_PART_NUMBER, partNumber);
    }
}
