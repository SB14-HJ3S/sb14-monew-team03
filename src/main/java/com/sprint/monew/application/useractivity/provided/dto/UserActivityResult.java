package com.sprint.monew.application.useractivity.provided.dto;

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
        List<ArticleViewActivityResult> articleViews) {}
