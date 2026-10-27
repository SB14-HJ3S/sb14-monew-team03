package com.sprint.monew.adapter.out.article;

import com.sprint.monew.application.article.required.ArticleInterestRepositoryPort;
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
}
