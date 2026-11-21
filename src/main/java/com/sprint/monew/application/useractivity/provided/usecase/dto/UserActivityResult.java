package com.sprint.monew.application.useractivity.provided.usecase.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserActivityResult(
        UUID id,
        String email,
        String nickname,
        Instant createdAt,
        List<SubscriptionActivityResult> subscriptions,
        List<CommentActivityResult> comments,
        List<CommentLikeActivityResult> commentLikes,
        List<ArticleViewActivityResult> articleViews) {

    /** 화면에 보여주는 최근 목록 개수 (요구사항: 각각 최대 10건) */
    public static final int RECENT_LIMIT = 10;

    /** 최근 댓글·좋아요·본 기사를 최신 RECENT_LIMIT건으로 자른다 (구독 목록은 전부) */
    public UserActivityResult limitRecent() {
        return new UserActivityResult(
                id,
                email,
                nickname,
                createdAt,
                subscriptions,
                comments.stream().limit(RECENT_LIMIT).toList(),
                commentLikes.stream().limit(RECENT_LIMIT).toList(),
                articleViews.stream().limit(RECENT_LIMIT).toList());
    }
}
