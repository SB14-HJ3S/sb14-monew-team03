package com.sprint.monew.adapter.out.article;

import com.sprint.monew.domain.article.entity.ArticleInterest;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ArticleInterestJpaRepository extends JpaRepository<ArticleInterest, UUID> {
    Optional<ArticleInterest> findByArticleIdAndInterestId(UUID articleId, UUID interestId);

    @Query(
            """
        delete from ArticleInterest a
        where a.interest.id = :interestId""")
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    void deleteAllByInterestId(UUID interestId);
}
