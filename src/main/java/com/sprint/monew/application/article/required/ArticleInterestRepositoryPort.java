package com.sprint.monew.application.article.required;

import java.util.UUID;

public interface ArticleInterestRepositoryPort {
    void deleteAllByInterestId(UUID interestId);
}
