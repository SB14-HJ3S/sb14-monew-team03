package com.sprint.monew.adapter.out.article;

import com.sprint.monew.application.article.required.ArticleInterestRepositoryPort;
import com.sprint.monew.domain.article.entity.ArticleInterest;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ArticleInterestRepositoryAdapter implements ArticleInterestRepositoryPort {

    private final ArticleInterestJpaRepository repository;

    @Override
    public void deleteAllByInterestId(UUID interestId) {
        repository.deleteAllByInterestId(interestId);
    }

    @Override
    public ArticleInterest save(ArticleInterest articleInterest) {
        return repository.save(articleInterest);
    }

    @Override
    public Optional<ArticleInterest> findByArticleIdAndInterestId(UUID articleId, UUID interestId) {
        return repository.findByArticleIdAndInterestId(articleId, interestId);
    }
}
