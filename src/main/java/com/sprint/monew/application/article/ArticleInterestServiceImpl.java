package com.sprint.monew.application.article;

import com.sprint.monew.application.article.provided.service.ArticleInterestService;
import com.sprint.monew.application.article.required.ArticleInterestRepositoryPort;
import com.sprint.monew.domain.article.entity.Article;
import com.sprint.monew.domain.article.entity.ArticleInterest;
import com.sprint.monew.domain.interest.entity.Interest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ArticleInterestServiceImpl implements ArticleInterestService {

    private final ArticleInterestRepositoryPort repository;

    @Override
    public Set<UUID> findLinkedArticleIds(UUID interestId, List<UUID> articleIds) {
        if (articleIds.isEmpty()) {
            return Set.of();
        }

        return new HashSet<>(repository.findLinkedArticleIds(interestId, articleIds));
    }

    @Override
    @Transactional
    public void createLinks(Interest interest, List<Article> articles) {
        if (articles.isEmpty()) {
            return;
        }

        List<ArticleInterest> links =
                articles.stream()
                        .map(article -> ArticleInterest.create(article, interest))
                        .toList();
        repository.saveAll(links);
    }

    @Override
    @Transactional
    public boolean createIfAbsent(Article article, Interest interest) {
        return repository
                .findByArticleIdAndInterestId(article.getId(), interest.getId())
                .map(existing -> false)
                .orElseGet(
                        () -> {
                            repository.save(ArticleInterest.create(article, interest));
                            return true;
                        });
    }

    @Override
    public void deleteAllByInterestId(UUID interestId) {
        repository.deleteAllByInterestId(interestId);
    }

    @Override
    public Map<UUID, List<UUID>> findInterestIdsByArticleIds(List<UUID> articleIds) {
        if (articleIds.isEmpty()) {
            return Map.of();
        }

        Map<UUID, List<UUID>> result = new HashMap<>();
        List<ArticleInterest> connections = repository.findAllByArticleIds(articleIds);

        for (ArticleInterest connection : connections) {
            UUID articleId = connection.getArticle().getId();
            UUID interestId = connection.getInterest().getId();

            List<UUID> interestIds = result.get(articleId);
            if (Objects.isNull(interestIds)) {
                interestIds = new ArrayList<>();
                result.put(articleId, interestIds);
            }

            interestIds.add(interestId);
        }

        return result;
    }
}
