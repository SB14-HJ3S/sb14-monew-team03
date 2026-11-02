package com.sprint.monew.adapter.out.useractivity.document;

import com.sprint.monew.application.useractivity.provided.dto.UserActivityResult;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "user_activities")
@CompoundIndexes({
    @CompoundIndex(name = "idx_comments_id", def = "{'comments.id': 1}"),
    @CompoundIndex(name = "idx_comments_article", def = "{'comments.articleId': 1}"),
    @CompoundIndex(name = "idx_likes_comment", def = "{'commentLikes.commentId': 1}"),
    @CompoundIndex(name = "idx_likes_comment_user", def = "{'commentLikes.commentUserId': 1}"),
    @CompoundIndex(name = "idx_likes_article", def = "{'commentLikes.articleId': 1}"),
    @CompoundIndex(name = "idx_subscriptions_interest", def = "{'subscriptions.interestId': 1}"),
    @CompoundIndex(name = "idx_views_article", def = "{'articleViews.articleId': 1}")
})
public record UserActivityDocument(
        @Id UUID id,
        String email,
        String nickname,
        Instant createdAt,
        List<SubscriptionItem> subscriptions,
        List<CommentItem> comments,
        List<CommentLikeItem> commentLikes,
        List<ArticleViewItem> articleViews) {
    public static final int RECENT_LIMIT = 10;

    public static final String FIELD_SUBSCRIPTIONS = "subscriptions";
    public static final String FIELD_COMMENTS = "comments";
    public static final String FIELD_COMMENT_LIKES = "commentLikes";
    public static final String FIELD_ARTICLE_VIEWS = "articleViews";

    public static UserActivityDocument create(
            UUID userId, String email, String nickname, Instant createdAt) {
        return new UserActivityDocument(
                userId, email, nickname, createdAt, List.of(), List.of(), List.of(), List.of());
    }

    public UserActivityResult toResult() {
        return new UserActivityResult(
                id,
                email,
                nickname,
                createdAt,
                subscriptions.stream().map(SubscriptionItem::toResult).toList(),
                comments.stream().map(CommentItem::toResult).toList(),
                commentLikes.stream().map(CommentLikeItem::toResult).toList(),
                articleViews.stream().map(ArticleViewItem::toResult).toList());
    }
}
