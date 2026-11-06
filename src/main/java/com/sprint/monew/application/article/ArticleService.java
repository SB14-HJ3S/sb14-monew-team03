package com.sprint.monew.application.article;

import com.sprint.monew.application.article.provided.dto.ArticleQuery;
import com.sprint.monew.domain.article.entity.Article;
import com.sprint.monew.global.pagination.PageResult;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ArticleService {

    List<Article> getArticlesForBackup(LocalDate date, UUID afterId, int limit);

    Article findById(UUID articleId);

    PageResult<Article> findPageByQuery(ArticleQuery query);

    Optional<Article> findBySourceUrl(String sourceUrl);

    List<Article> findAllBySourceUrls(List<String> sourceUrls);

    Article save(Article article);

    void increaseCommentCount(UUID articleId);

    void decreaseCommentCount(UUID articleId);

    // 사용자 완전 삭제 전에, 탈퇴자가 쓴 댓글 수만큼 기사마다 댓글 수를 줄임
    void decreaseCommentCountsByUserIds(List<UUID> userIds);

    void increaseViewCount(UUID articleId);

    void softDelete(UUID articleId);

    void hardDelete(UUID articleId);
}
