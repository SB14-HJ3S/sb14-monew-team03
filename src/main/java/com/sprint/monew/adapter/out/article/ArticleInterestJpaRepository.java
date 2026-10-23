package com.sprint.monew.adapter.out.article;

import com.sprint.monew.domain.article.entity.ArticleInterest;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleInterestJpaRepository extends JpaRepository<ArticleInterest, UUID> {
    Optional<ArticleInterest> findByArticleIdAndInterestId(UUID articleId, UUID interestId);
}
