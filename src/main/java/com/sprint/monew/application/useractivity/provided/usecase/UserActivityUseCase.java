package com.sprint.monew.application.useractivity.provided.usecase;

import com.sprint.monew.application.useractivity.provided.usecase.dto.ArticleViewActivityResult;
import com.sprint.monew.application.useractivity.provided.usecase.dto.CommentActivityResult;
import com.sprint.monew.application.useractivity.provided.usecase.dto.CommentLikeActivityResult;
import com.sprint.monew.application.useractivity.provided.usecase.dto.SubscriptionActivityResult;
import com.sprint.monew.application.useractivity.provided.usecase.dto.UserActivityResult;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface UserActivityUseCase {
    /** 사용자의 활동 내역을 조회한다. 없거나 탈퇴한 사용자면 404. */
    UserActivityResult findByUserId(@NotNull UUID userId);

    // 사용자
    void userRegistered(
            @NotNull UUID userId,
            @NotBlank String email,
            @NotBlank String nickname,
            @NotNull Instant createdAt);

    void nicknameUpdated(@NotNull UUID userId, @NotBlank String nickname);

    void usersHardDeleted(@NotNull List<@NotNull UUID> userIds);

    // 구독 관심사
    void subscribed(@NotNull UUID userId, @NotNull SubscriptionActivityResult item);

    void unsubscribed(@NotNull UUID userId, @NotNull UUID interestId);

    void interestKeywordsUpdated(
            @NotNull UUID interestId, @NotNull List<@NotBlank String> keywords);

    void interestDeleted(@NotNull UUID interestId);

    // 댓글
    void commentCreated(@NotNull UUID userId, @NotNull CommentActivityResult item);

    void commentUpdated(@NotNull UUID commentId, @NotBlank String content);

    void commentDeleted(@NotNull UUID commentId);

    // 좋아요
    void commentLiked(@NotNull UUID likedById, @NotNull CommentLikeActivityResult item);

    void commentUnliked(@NotNull UUID likedById, @NotNull UUID commentId, @Min(0) long likeCount);

    // 기사
    void articleViewed(@NotNull UUID userId, @NotNull ArticleViewActivityResult item);

    void articleDeleted(@NotNull UUID articleId);
}
