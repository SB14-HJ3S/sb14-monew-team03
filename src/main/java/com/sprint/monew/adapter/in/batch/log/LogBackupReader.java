package com.sprint.monew.adapter.in.batch.log;

import com.sprint.monew.application.log.provided.LogBackupUseCase;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@StepScope
@Component
@RequiredArgsConstructor
public class LogBackupReader implements ItemStreamReader<List<Path>> {

    private final LogBackupUseCase useCase;

    @Value("#{jobParameters['logTargetDate']}")
    private LocalDate targetDate;

    @Value("${monew.log.backup.limit}")
    private int limit;

    private int afterIndex = -1;

    @Override
    public List<Path> read() throws IOException {
        List<Path> files = useCase.readLogs(targetDate, afterIndex, limit).logPath();
        if (files.isEmpty()) {
            return null;
        }

        afterIndex = getLastFileIndex(files);
        return files;
    }

    private int getLastFileIndex(List<Path> files) {
        String[] parts = files.get(files.size() - 1).getFileName().toString().split("\\.");
        return Integer.parseInt(parts[parts.length - 2]);
    }

    static final String AFTER_INDEX = "logBackupReader.afterIndex";
    static final String LIMIT = "logBackupReader.limit";

    @Override
    public void open(ExecutionContext executionContext) {
        afterIndex = executionContext.getInt(AFTER_INDEX, -1);
        limit = executionContext.getInt(LIMIT, limit);
    }

    @Override
    public void update(ExecutionContext executionContext) {
        if (afterIndex >= 0) {
            executionContext.putInt(AFTER_INDEX, afterIndex);
        }
        executionContext.putInt(LIMIT, limit);
    }
}
