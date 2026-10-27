package com.sprint.monew.application.article;

import com.sprint.monew.application.article.required.ArticleInterestRepositoryPort;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ArticleInterestService implements IArticleInterestService {

    private final ArticleInterestRepositoryPort repository;

    @Override
    public void deleteAllByInterestId(UUID interestId) {
        repository.deleteAllByInterestId(interestId);
    }
}
