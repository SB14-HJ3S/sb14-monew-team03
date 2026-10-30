package com.sprint.monew.adapter.in.controller.useractivity.dto.response;

import com.sprint.monew.application.useractivity.provided.dto.UserActivityResult;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserActivityDto(
        UUID id,
        String email,
        String nickname,
        Instant createdAt,
        List<SubscriptionActivityDto> subscriptions,
        List<CommentActivityDto> comments,
        List<CommentLikeActivityDto> commentLikes,
        List<ArticleViewActivityDto> articleViews) {

    public static UserActivityDto from(UserActivityResult r) {
        return new UserActivityDto(
                r.id(),
                r.email(),
                r.nickname(),
                r.createdAt(),
                r.subscriptions().stream().map(SubscriptionActivityDto::from).toList(),
                r.comments().stream().map(CommentActivityDto::from).toList(),
                r.commentLikes().stream().map(CommentLikeActivityDto::from).toList(),
                r.articleViews().stream().map(ArticleViewActivityDto::from).toList());
    }
} // 최근 본 기사 (최대 10건)
