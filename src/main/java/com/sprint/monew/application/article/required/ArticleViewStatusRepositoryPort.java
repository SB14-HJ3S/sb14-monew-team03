package com.sprint.monew.application.article.required;

import com.sprint.monew.domain.article.entity.ArticleViewStatus;
import java.util.*;

public interface ArticleViewStatusRepositoryPort {

    // JPA 기본 메서드
    ArticleViewStatus save(ArticleViewStatus entity);

    Optional<ArticleViewStatus> findById(UUID id);

    List<ArticleViewStatus> findAll();

    void deleteById(UUID id);

    // 커스텀
    Set<UUID> findViewedArticleIds(UUID userId, Collection<UUID> articleIds);
}
