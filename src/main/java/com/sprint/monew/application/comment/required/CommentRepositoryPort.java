package com.sprint.monew.application.comment.required;

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
}
