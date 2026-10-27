package com.sprint.monew.application.article;

import java.util.UUID;

public interface IArticleInterestService {
    void deleteAllByInterestId(UUID interestId);
}
