package com.sprint.monew.adapter.out.article;

import com.sprint.monew.domain.article.entity.Article;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleJpaRepository extends JpaRepository<Article, UUID> {}
