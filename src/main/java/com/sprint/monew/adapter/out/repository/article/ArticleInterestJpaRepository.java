package com.sprint.monew.adapter.out.repository.article;

import com.sprint.monew.domain.article.entity.ArticleInterest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArticleInterestJpaRepository extends JpaRepository<ArticleInterest, UUID> {
    @Query(
            """
            select ai.article.id
            from ArticleInterest ai
            where ai.interest.id = :interestId
              and ai.article.id in :articleIds
            """)
    List<UUID> findLinkedArticleIds(
            @Param("interestId") UUID interestId, @Param("articleIds") List<UUID> articleIds);

    Optional<ArticleInterest> findByArticleIdAndInterestId(UUID articleId, UUID interestId);

    @Query(
            """
            delete from ArticleInterest a
            where a.interest.id = :interestId""")
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    void deleteAllByInterestId(UUID interestId);

    List<ArticleInterest> findAllByArticleIdIn(List<UUID> articleIds);
}
