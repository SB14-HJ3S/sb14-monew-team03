package com.sprint.monew.application.useractivity;

import com.sprint.monew.application.useractivity.provided.UserActivityUseCase;
import com.sprint.monew.application.useractivity.provided.dto.ArticleViewActivityResult;
import com.sprint.monew.application.useractivity.provided.dto.CommentActivityResult;
import com.sprint.monew.application.useractivity.provided.dto.CommentLikeActivityResult;
import com.sprint.monew.application.useractivity.provided.dto.SubscriptionActivityResult;
import com.sprint.monew.application.useractivity.provided.dto.UserActivityResult;
import com.sprint.monew.application.useractivity.required.UserActivityReader;
import com.sprint.monew.application.useractivity.required.UserActivityWriter;
import com.sprint.monew.global.annotation.ApplicationService;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.UserErrorType;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@ApplicationService
@RequiredArgsConstructor
public class UserActivityApplication implements UserActivityUseCase {

    private final UserActivityReader userActivityReader;
    private final UserActivityWriter writer;

    @Override
    @Transactional(readOnly = true)
    public UserActivityResult findByUserId(UUID userId) {
        return userActivityReader
                .findByUserId(userId)
                .orElseThrow(
                        () ->
                                new MonewException(
                                        UserErrorType.USER_NOT_FOUND, Map.of("userId", userId)));
    }

    // ===== 사용자 =====

    @Override
    public void userRegistered(UUID userId, String email, String nickname, Instant createdAt) {
        writer.create(userId, email, nickname, createdAt);
    }

    @Override
    public void nicknameUpdated(UUID userId, String nickname) {
        writer.updateNickname(userId, nickname);
    }

    @Override
    public void usersHardDeleted(List<UUID> userIds) {
        writer.deleteAll(userIds);
        // 그 사용자들의 댓글이 CASCADE로 지워지므로, 다른 사람 "좋아요한 댓글"에서도 뺀다
        writer.removeCommentLikesOfCommentUsers(userIds);
    }

    // ===== 구독 관심사 =====

    @Override
    public void subscribed(UUID userId, SubscriptionActivityResult item) {
        writer.addSubscription(userId, item);
    }

    @Override
    public void unsubscribed(UUID userId, UUID interestId) {
        writer.removeSubscription(userId, interestId);
    }

    @Override
    public void interestKeywordsUpdated(UUID interestId, List<String> keywords) {
        writer.updateInterestKeywords(interestId, keywords);
    }

    @Override
    public void interestDeleted(UUID interestId) {
        writer.removeInterest(interestId);
    }

    // ===== 댓글 =====

    @Override
    public void commentCreated(UUID userId, CommentActivityResult item) {
        writer.addComment(userId, item);
    }

    @Override
    public void commentUpdated(UUID commentId, String content) {
        writer.updateCommentContent(commentId, content);
    }

    @Override
    public void commentDeleted(UUID commentId) {
        writer.removeComment(commentId);
    }

    // ===== 좋아요 =====

    @Override
    public void commentLiked(UUID likedById, CommentLikeActivityResult item) {
        writer.addCommentLike(likedById, item);
    }

    @Override
    public void commentUnliked(UUID likedById, UUID commentId) {
        writer.removeCommentLike(likedById, commentId);
    }

    // ===== 기사 =====

    @Override
    public void articleViewed(UUID userId, ArticleViewActivityResult item) {
        writer.addArticleView(userId, item);
    }

    @Override
    public void articleDeleted(UUID articleId) {
        writer.removeArticle(articleId);
    }
}
