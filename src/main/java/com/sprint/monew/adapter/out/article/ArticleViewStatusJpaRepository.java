package com.sprint.monew.adapter.out.article;

import com.sprint.monew.domain.article.entity.ArticleViewStatus;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArticleViewStatusJpaRepository extends JpaRepository<ArticleViewStatus, UUID> {
    @Query(
            """
        select v.article.id
        from ArticleViewStatus v
        where v.user.id = :userId
          and v.article.id in :articleIds
        """)
    Set<UUID> findViewedArticleIds(
            @Param("userId") UUID userId, @Param("articleIds") Collection<UUID> articleIds);
}
