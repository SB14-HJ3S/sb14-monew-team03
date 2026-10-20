package com.sprint.monew.adapter.in.controller.comment.dto.response;

import java.time.Instant;
import java.util.UUID;

public record CommentDto(
        UUID id, // 댓글 ID
        UUID articleId, // 기사 ID
        UUID userId, // 작성자 ID
        String userNickname, // 작성자 닉네임
        String content, // 내용
        Long likeCount, // 좋아요 수
        boolean likedByMe, // 요청자의 좋아요 여부
        Instant createdAt) {} // 작성된 날짜
