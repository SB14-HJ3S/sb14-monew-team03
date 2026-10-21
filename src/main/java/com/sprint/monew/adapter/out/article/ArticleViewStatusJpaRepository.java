package com.sprint.monew.adapter.out.article;

import com.sprint.monew.domain.article.entity.ArticleViewStatus;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ArticleViewStatusJpaRepository extends JpaRepository<ArticleViewStatus, UUID> {
    @Query() // TODO
    Set<UUID> findViewedArticleIds(UUID userId, Collection<UUID> articleIds);
}
