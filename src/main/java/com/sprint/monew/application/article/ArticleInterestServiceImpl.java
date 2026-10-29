package com.sprint.monew.application.article;

import com.sprint.monew.application.article.required.ArticleInterestRepositoryPort;
import com.sprint.monew.domain.article.entity.Article;
import com.sprint.monew.domain.article.entity.ArticleInterest;
import com.sprint.monew.domain.interest.entity.Interest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ArticleInterestServiceImpl implements ArticleInterestService {

    private final ArticleInterestRepositoryPort repository;

    @Override
    @Transactional
    public ArticleInterest getOrCreateArticleInterest(Article article, Interest interest) {
        return repository
                .findByArticleIdAndInterestId(article.getId(), interest.getId())
                .orElseGet(() -> repository.save(ArticleInterest.create(article, interest)));
    }

    @Override
    public void deleteAllByInterestId(UUID interestId) {
        repository.deleteAllByInterestId(interestId);
    }
}
