package com.sprint.monew.adapter.in.controller.useractivity.dto.response;

import com.sprint.monew.application.useractivity.provided.dto.CommentActivityResult;
import java.time.Instant;
import java.util.UUID;

public record CommentActivityDto(
        UUID id, // 댓글 ID
        UUID articleId, // 기사 ID
        String articleTitle, // 기사 제목
        UUID userId, // 작성자 ID
        String userNickname, // 작성자 닉네임
        String content, // 내용
        Long likeCount, // 좋아요 수
        Instant createdAt) {

    public static CommentActivityDto from(CommentActivityResult r) {
        return new CommentActivityDto(
                r.id(),
                r.articleId(),
                r.articleTitle(),
                r.userId(),
                r.userNickname(),
                r.content(),
                r.likeCount(),
                r.createdAt());
    }
}
