package com.sprint.monew.adapter.in.batch.article.backup;

import com.sprint.monew.application.article.provided.usecase.ArticleBackupUseCase;
import com.sprint.monew.application.article.provided.usecase.dto.ArticleResult;
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
public class ArticleBackupWriter implements ItemStreamWriter<List<ArticleResult>> {

    private final ArticleBackupUseCase useCase;

    @Value("#{jobParameters['targetDate']}")
    private LocalDate targetDate;

    private long partNumber = 1L;

    @Override
    public void write(Chunk<? extends List<ArticleResult>> chunk) {
        for (List<ArticleResult> backingUp : chunk) {
            useCase.backupChunk(targetDate, partNumber, backingUp);
            partNumber++;
        }
    }

    static final String NEXT_PART_NUMBER = "articleBackupWriter.nextPartNumber";

    @Override
    public void open(ExecutionContext executionContext) {
        partNumber = executionContext.getLong(NEXT_PART_NUMBER, 1L);
    }

    @Override
    public void update(ExecutionContext executionContext) {
        executionContext.putLong(NEXT_PART_NUMBER, partNumber);
    }
}
