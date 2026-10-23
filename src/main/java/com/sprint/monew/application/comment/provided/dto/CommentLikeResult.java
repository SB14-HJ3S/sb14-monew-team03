package com.sprint.monew.application.comment.provided.dto;

import com.sprint.monew.domain.comment.entity.Comment;
import com.sprint.monew.domain.comment.entity.CommentLike;
import java.time.Instant;
import java.util.UUID;

// 댓글 좋아요 결과 — OSIV가 꺼져 있어 댓글·작성자 정보를 트랜잭션 안에서 채워 넘긴다
public record CommentLikeResult(
        UUID id,
        UUID likedBy,
        Instant createdAt,
        UUID commentId,
        UUID articleId,
        UUID commentUserId,
        String commentUserNickname,
        String commentContent,
        long commentLikeCount,
        Instant commentCreatedAt) {

    // 댓글·작성자·기사를 지연 로딩으로 읽으므로 트랜잭션 안에서 호출해야 한다
    // 좋아요 수를 DB에서 올려서, 최신 숫자가 담긴 댓글을 따로 받는다
    public static CommentLikeResult of(CommentLike commentLike, Comment comment) {
        return new CommentLikeResult(
                commentLike.getId(),
                commentLike.getUser().getId(),
                commentLike.getCreatedAt(),
                comment.getId(),
                comment.getArticle().getId(),
                comment.getUser().getId(),
                comment.getUser().getNickname(),
                comment.getContent(),
                comment.getLikeCount(),
                comment.getCreatedAt());
    }
}
