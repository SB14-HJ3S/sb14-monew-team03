package com.sprint.monew.application.comment.required;

import com.sprint.monew.application.comment.provided.dto.CommentQuery;
import com.sprint.monew.domain.comment.entity.Comment;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CommentRepositoryPort {
    // JPA 기본 메서드
    Comment save(Comment entity);

    Optional<Comment> findById(UUID id);

    List<Comment> findAll();

    void deleteById(UUID id);

    // 동시에 다른 사람이 눌러도 숫자가 사라지지 않게 DB에서 +1 -1
    int increaseLikeCount(UUID commentId);

    int decreaseLikeCount(UUID commentId);

    // 살아 있을 때만 삭제 시각을 넣는다. 동시에 지워도 한 요청만 1을 받는다
    int softDeleteIfActive(UUID commentId, Instant deletedAt);

    // 물리 삭제용. 동시에 지워서 이미 없으면 예외 없이 0을 받는다
    int hardDeleteById(UUID commentId);

    // 목록 조회용. 다음 페이지 여부를 알려고 limit + 1개까지 가져온다
    // cursorComment: 이전 페이지 마지막 댓글. 첫 페이지면 null
    List<Comment> findByQuery(CommentQuery query, Comment cursorComment);

    long countByQuery(CommentQuery query);

    // 기사 물리 삭제 때 같이 지워질 댓글 찾기용. 논리 삭제된 댓글도 포함한다
    List<UUID> findIdsByArticleId(UUID articleId);

    // 사용자 완전 삭제 전에, 탈퇴자가 누른 좋아요 수만큼 댓글마다 한 번에 줄임. 바뀐 댓글 수를 돌려줌
    int decreaseLikeCountsByUserIds(List<UUID> userIds);
}
