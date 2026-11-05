package com.sprint.monew.adapter.out.useractivity;

import static com.sprint.monew.domain.article.entity.QArticle.article;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sprint.monew.application.useractivity.required.ArticleStatReader;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ArticleStatRdbAdapter implements ArticleStatReader {

    private final JPAQueryFactory queryFactory;

    @Override
    public Map<UUID, ArticleStat> findStats(Collection<UUID> articleIds) {
        if (articleIds.isEmpty()) {
            return Map.of();
        }
        return queryFactory
                .select(article.id, article.viewCount, article.commentCount)
                .from(article)
                .where(article.id.in(articleIds))
                .fetch()
                .stream()
                .collect(
                        Collectors.toMap(
                                r -> r.get(article.id),
                                r ->
                                        new ArticleStat(
                                                r.get(article.viewCount),
                                                r.get(article.commentCount))));
    }
}
