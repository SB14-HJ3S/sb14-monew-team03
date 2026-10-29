package com.sprint.monew.application.article;

import com.sprint.monew.application.article.provided.dto.ArticleQuery;
import com.sprint.monew.application.article.provided.dto.CollectedArticle;
import com.sprint.monew.domain.article.entity.Article;
import com.sprint.monew.global.pagination.PageResult;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ArticleService {

    Article findById(UUID articleId);

    PageResult<Article> findPageByQuery(ArticleQuery query);

    List<CollectedArticle> collectNaverArticles(String keyword);

    Optional<Article> saveCollectedArticle(CollectedArticle collectedArticle);

    void increaseCommentCount(UUID articleId);

    Optional<Article> findBySourceUrl(String sourceUrl);

    Article createCollectedArticle(CollectedArticle collectedArticle);

    void publishArticlesSaved(UUID interestId, String interestName, int articleCount);
}
