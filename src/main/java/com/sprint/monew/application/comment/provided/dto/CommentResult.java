package com.sprint.monew.application.comment.provided.dto;

import java.time.Instant;
import java.util.UUID;

// 댓글 조회·수정 결과 — OSIV가 꺼져 있어 연관 엔티티(작성자 닉네임)와 요청자 좋아요 여부를 트랜잭션 안에서 채워 넘긴다
public record CommentResult(
        UUID id,
        UUID articleId,
        UUID userId,
        String userNickname,
        String content,
        long likeCount,
        boolean likedByMe,
        Instant createdAt) {}
