package com.sprint.monew.adapter.in.batch.article.backup;

import com.sprint.monew.application.article.provided.ArticleBackupUseCase;
import com.sprint.monew.application.article.provided.dto.ArticleResult;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@StepScope
@Component
@RequiredArgsConstructor
public class ArticleBackupReader implements ItemStreamReader<List<ArticleResult>> {

    private final ArticleBackupUseCase useCase;

    @Value("#{jobParameters['targetDate']}")
    private LocalDate targetDate;

    @Value("${monew.article.limit}")
    private int limit;

    private UUID afterId = null;

    @Override
    public List<ArticleResult> read() {
        List<ArticleResult> articles = useCase.readArticles(targetDate, afterId, limit);

        if (articles.isEmpty()) {
            return null;
        }

        afterId = getLastArticleId(articles);
        return articles;
    }

    private UUID getLastArticleId(List<ArticleResult> articles) {
        return articles.get(articles.size() - 1).id();
    }

    static final String AFTER_ID = "articleBackupReader.afterId";
    static final String LIMIT = "articleBackupReader.limit";

    @Override
    public void open(@NonNull ExecutionContext executionContext) {
        String savedAfterId = executionContext.getString(AFTER_ID, "");
        if (StringUtils.hasText(savedAfterId)) {
            afterId = UUID.fromString(savedAfterId);
        }
        limit = executionContext.getInt(LIMIT, limit);
    }

    @Override
    public void update(@NonNull ExecutionContext executionContext) {
        if (Objects.nonNull(afterId)) {
            executionContext.putString(AFTER_ID, afterId.toString());
        }
        executionContext.putInt(LIMIT, limit);
    }
}
