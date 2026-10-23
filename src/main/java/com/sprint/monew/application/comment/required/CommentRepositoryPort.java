package com.sprint.monew.application.comment.required;

import com.sprint.monew.domain.comment.entity.Comment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CommentRepositoryPort {
    // JPA 기본 메서드
    Comment save(Comment entity);

    Optional<Comment> findById(UUID id);

    List<Comment> findAll();

    void deleteById(UUID id);

    // 동시에 다룬 사람이 눌러도 숫자가 사라지지 않게 DB에서 +1 -1
    int increaseLikeCount(UUID commentId);

    int decreaseLikeCount(UUID commentId);
}
