package com.sprint.monew.application.useractivity.provided.usecase;

import com.sprint.monew.application.useractivity.provided.usecase.dto.ArticleViewActivityResult;
import com.sprint.monew.application.useractivity.provided.usecase.dto.CommentActivityResult;
import com.sprint.monew.application.useractivity.provided.usecase.dto.CommentLikeActivityResult;
import com.sprint.monew.application.useractivity.provided.usecase.dto.SubscriptionActivityResult;
import com.sprint.monew.application.useractivity.provided.usecase.dto.UserActivityResult;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface UserActivityUseCase {
    /** 사용자의 활동 내역을 조회한다. 없거나 탈퇴한 사용자면 404. */
    UserActivityResult findByUserId(UUID userId);

    // 사용자
    void userRegistered(UUID userId, String email, String nickname, Instant createdAt);

    void nicknameUpdated(UUID userId, String nickname);

    void usersHardDeleted(List<UUID> userIds);

    // 구독 관심사
    void subscribed(UUID userId, SubscriptionActivityResult item);

    void unsubscribed(UUID userId, UUID interestId);

    void interestKeywordsUpdated(UUID interestId, List<String> keywords);

    void interestDeleted(UUID interestId);

    // 댓글
    void commentCreated(UUID userId, CommentActivityResult item);

    void commentUpdated(UUID commentId, String content);

    void commentDeleted(UUID commentId);

    // 좋아요
    void commentLiked(UUID likedById, CommentLikeActivityResult item);

    void commentUnliked(UUID likedById, UUID commentId, long likeCount);

    // 기사
    void articleViewed(UUID userId, ArticleViewActivityResult item);

    void articleDeleted(UUID articleId);
}
