package com.sprint.monew.adapter.in.batch.article.backup;

import com.sprint.monew.application.article.provided.ArticleBackupUseCase;
import com.sprint.monew.application.article.provided.dto.ArticleResult;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@StepScope
@Component
@RequiredArgsConstructor
public class ArticleBackupReader implements ItemReader<List<ArticleResult>> {

    private final ArticleBackupUseCase useCase;

    @Value("${monew.article.limit}")
    private int limit;

    private UUID afterId = null;

    @Override
    public List<ArticleResult> read()
            throws Exception,
                    UnexpectedInputException,
                    ParseException,
                    NonTransientResourceException {
        LocalDate targetDate = LocalDate.now(ZoneId.systemDefault()).minusDays(1L);
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
}
