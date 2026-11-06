package com.sprint.monew.application.article.required;

import com.sprint.monew.application.article.provided.dto.ArticleQuery;
import com.sprint.monew.domain.article.entity.Article;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ArticleRepositoryPort {
    // JPA 기본 메서드
    Article save(Article entity);

    Optional<Article> findById(UUID id);

    List<Article> findAll();

    void deleteById(UUID id);

    // 본인 쓸 거
    List<Article> findByQuery(ArticleQuery query);

    long countByQuery(ArticleQuery query);

    Optional<Article> findBySourceUrl(String sourceUrl);

    List<Article> findAllBySourceUrlIn(Collection<String> sourceUrls);

    // 동시에 댓글이 달려도 숫자가 사라지지 않게 DB에서 +1
    int increaseCommentCount(UUID articleId);

    // 동시에 댓글이 지워져도 숫자가 사라지지 않게 DB에서 -1
    int decreaseCommentCount(UUID articleId);

    // 사용자 완전 삭제 전에, 탈퇴자가 쓴 댓글 수만큼 기사마다 한 번에 줄임. 바뀐 기사 수를 돌려줌
    int decreaseCommentCountsByUserIds(List<UUID> userIds);

    int softDeleteIfActive(UUID articleId, Instant deletedAt);

    List<Article> findBackupChunk(LocalDateTime from, LocalDateTime to, UUID afterId, int limit);

    int increaseViewCount(UUID articleId);

    List<Article> saveAll(List<Article> articles);
}
