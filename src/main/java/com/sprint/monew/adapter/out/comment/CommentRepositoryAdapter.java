package com.sprint.monew.adapter.out.comment;

import com.sprint.monew.application.comment.provided.dto.CommentQuery;
import com.sprint.monew.application.comment.required.CommentRepositoryPort;
import com.sprint.monew.domain.comment.entity.Comment;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CommentRepositoryAdapter implements CommentRepositoryPort {

    private final CommentJpaRepository jpaRepository;

    @Override
    public Comment save(Comment entity) {
        return jpaRepository.save(entity);
    }

    @Override
    public Optional<Comment> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<Comment> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public int increaseLikeCount(UUID commentId) {
        return jpaRepository.increaseLikeCount(commentId);
    }

    @Override
    public int decreaseLikeCount(UUID commentId) {
        return jpaRepository.decreaseLikeCount(commentId);
    }

    @Override
    public int softDeleteIfActive(UUID commentId, Instant deletedAt) {
        return jpaRepository.softDeleteIfActive(commentId, deletedAt);
    }

    @Override
    public int hardDeleteById(UUID commentId) {
        return jpaRepository.hardDeleteById(commentId);
    }

    @Override
    public List<Comment> findByQuery(CommentQuery query, Comment cursorComment) {
        return jpaRepository.findByQuery(query, cursorComment);
    }

    @Override
    public long countByQuery(CommentQuery query) {
        return jpaRepository.countByQuery(query);
    }

    @Override
    public List<UUID> findIdsByArticleId(UUID articleId) {
        return jpaRepository.findIdsByArticleId(articleId);
    }

    @Override
    public int decreaseLikeCountsByUserIds(List<UUID> userIds) {
        return jpaRepository.decreaseLikeCountsByUserIds(userIds);
    }
}
