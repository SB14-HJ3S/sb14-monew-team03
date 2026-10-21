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
}
