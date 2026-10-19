package com.sprint.monew.domain.useractivity.dto.response;

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
        Instant commentCreatedAt) {} // 작성된 날짜
