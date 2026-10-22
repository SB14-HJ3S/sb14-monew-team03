package com.sprint.monew.application.comment.provided.dto;

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
        Instant commentCreatedAt) {}
