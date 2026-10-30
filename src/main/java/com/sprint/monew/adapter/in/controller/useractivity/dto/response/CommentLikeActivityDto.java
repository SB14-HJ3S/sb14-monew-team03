package com.sprint.monew.adapter.in.controller.useractivity.dto.response;

import com.sprint.monew.application.useractivity.provided.dto.CommentLikeActivityResult;
import java.time.Instant;
import java.util.UUID;

public record CommentLikeActivityDto(
        UUID id, // 좋아요 ID
        Instant createdAt, // 좋아요한 날짜
        UUID commentId, // 댓글 ID
        UUID articleId, // 기사 ID
        String articleTitle, // 기사 제목
        UUID commentUserId, // 작성자 ID
        String commentUserNickname, // 작성자 닉네임
        String commentContent, // 내용
        Long commentLikeCount, // 좋아요 수
        Instant commentCreatedAt) {

    public static CommentLikeActivityDto from(CommentLikeActivityResult r) {
        return new CommentLikeActivityDto(
                r.id(),
                r.createdAt(),
                r.commentId(),
                r.articleId(),
                r.articleTitle(),
                r.commentUserId(),
                r.commentUserNickname(),
                r.commentContent(),
                r.commentLikeCount(),
                r.commentCreatedAt());
    }
} // 작성된 날짜
