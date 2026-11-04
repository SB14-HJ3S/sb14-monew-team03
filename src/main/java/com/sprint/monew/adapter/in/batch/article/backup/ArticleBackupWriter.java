package com.sprint.monew.adapter.in.batch.article.backup;

import com.sprint.monew.application.article.provided.ArticleBackupUseCase;
import com.sprint.monew.application.article.provided.dto.ArticleBackupResult;
import com.sprint.monew.application.article.provided.dto.ArticleResult;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@StepScope
@Component
@RequiredArgsConstructor
public class ArticleBackupWriter implements ItemWriter<List<ArticleResult>> {

    private final ArticleBackupUseCase useCase;

    private long partNumber = 1L;

    @Override
    public void write(Chunk<? extends List<ArticleResult>> chunk) throws Exception {
        LocalDate targetDate = LocalDate.now(ZoneId.systemDefault()).minusDays(1L);
        for (List<ArticleResult> backingUp : chunk) {
            ArticleBackupResult articleBackupResult =
                    useCase.backupChunk(targetDate, partNumber++, backingUp);
        }
    }
}
