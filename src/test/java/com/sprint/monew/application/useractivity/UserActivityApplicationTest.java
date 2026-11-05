package com.sprint.monew.application.useractivity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sprint.monew.application.user.IUserService;
import com.sprint.monew.application.useractivity.provided.dto.ArticleViewActivityResult;
import com.sprint.monew.application.useractivity.provided.dto.CommentActivityResult;
import com.sprint.monew.application.useractivity.provided.dto.CommentLikeActivityResult;
import com.sprint.monew.application.useractivity.provided.dto.SubscriptionActivityResult;
import com.sprint.monew.application.useractivity.provided.dto.UserActivityResult;
import com.sprint.monew.application.useractivity.required.ArticleStatReader;
import com.sprint.monew.application.useractivity.required.ArticleStatReader.ArticleStat;
import com.sprint.monew.application.useractivity.required.UserActivityReader;
import com.sprint.monew.application.useractivity.required.UserActivitySourceReader;
import com.sprint.monew.application.useractivity.required.UserActivityWriter;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.UserErrorType;
import com.sprint.monew.global.type.ArticleSourceType;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserActivityApplicationTest {

    @Mock private UserActivityReader userActivityReader;
    @Mock private UserActivitySourceReader userActivitySourceReader;
    @Mock private UserActivityWriter writer;
    @Mock private IUserService userService;
    @Mock private ArticleStatReader articleStatReader;

    @InjectMocks private UserActivityApplication userActivityApplication;

    @Test
    @DisplayName("Mongo 문서도 원본 활동 내역도 없으면 USER_NOT_FOUND 예외를 던지고 저장하지 않는다.")
    void givenNoDocumentAndNoSource_whenFind_thenThrowsUserNotFound() {
        UUID userId = UUID.randomUUID();
        when(userActivityReader.findByUserId(userId)).thenReturn(Optional.empty()); // Mongo 문서 없음
        when(userActivitySourceReader.findByUserId(userId)).thenReturn(Optional.empty()); // 원본에도 없음

        assertThatThrownBy(() -> userActivityApplication.findByUserId(userId))
                .isInstanceOf(MonewException.class)
                .extracting("errorType")
                .isEqualTo(UserErrorType.USER_NOT_FOUND);
        verifyNoInteractions(writer); // 아무것도 저장하지 않음
    }

    @Test
    @DisplayName("사용자 완전 삭제는 문서 삭제 후 다른 사람 문서의 그 사람들 댓글 좋아요까지 정리한다.")
    void whenUsersHardDeleted_thenDeleteDocumentsAndCleanLikes() {
        List<UUID> userIds = List.of(UUID.randomUUID(), UUID.randomUUID());

        userActivityApplication.usersHardDeleted(userIds);

        InOrder order = inOrder(writer);
        order.verify(writer).deleteAll(userIds);
        order.verify(writer).removeCommentLikesOfCommentUsers(userIds);
    }

    @Test
    @DisplayName("댓글 작성은 작성자 문서에 댓글을 추가한다.")
    void whenCommentCreated_thenAddComment() {
        UUID userId = UUID.randomUUID();
        CommentActivityResult item =
                new CommentActivityResult(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "기사",
                        userId,
                        "우디",
                        "내용",
                        0L,
                        Instant.now());

        userActivityApplication.commentCreated(userId, item);

        verify(writer).addComment(userId, item);
    }

    @Test
    @DisplayName("좋아요는 항목을 추가하고, 그 댓글의 좋아요 수를 모든 문서에 맞춘다.")
    void whenCommentLiked_thenAddLikeAndUpdateCount() {
        UUID likedById = UUID.randomUUID();
        UUID commentId = UUID.randomUUID();
        CommentLikeActivityResult item =
                new CommentLikeActivityResult(
                        UUID.randomUUID(),
                        Instant.now(),
                        commentId,
                        UUID.randomUUID(),
                        "기사",
                        UUID.randomUUID(),
                        "버즈",
                        "내용",
                        4L,
                        Instant.now());

        userActivityApplication.commentLiked(likedById, item);

        verify(writer).addCommentLike(likedById, item);
        verify(writer).updateCommentLikeCount(commentId, 4L);
    }

    @Test
    @DisplayName("좋아요 취소는 항목을 빼고, 그 댓글의 좋아요 수를 모든 문서에 맞춘다.")
    void whenCommentUnliked_thenRemoveLikeAndUpdateCount() {
        UUID likedById = UUID.randomUUID();
        UUID commentId = UUID.randomUUID();

        userActivityApplication.commentUnliked(likedById, commentId, 3L);

        verify(writer).removeCommentLike(likedById, commentId);
        verify(writer).updateCommentLikeCount(commentId, 3L);
    }

    @Test
    @DisplayName("Mongo 문서가 없으면 원본을 그대로 저장하고, 응답은 최근 10건만 돌려준다.")
    void givenNoDocument_whenFind_thenSavesAllAndReturnsLatestTen() {
        UUID userId = UUID.randomUUID();
        List<CommentActivityResult> fifteen =
                IntStream.range(0, 15)
                        .mapToObj(
                                i ->
                                        new CommentActivityResult(
                                                UUID.randomUUID(),
                                                UUID.randomUUID(),
                                                "기사",
                                                userId,
                                                "우디",
                                                "댓글" + i,
                                                0L,
                                                Instant.now()))
                        .toList();
        UserActivityResult source =
                new UserActivityResult(
                        userId,
                        "woody@monew.com",
                        "우디",
                        Instant.now(),
                        List.of(),
                        fifteen,
                        List.of(),
                        List.of());
        when(userActivityReader.findByUserId(userId)).thenReturn(Optional.empty());
        when(userActivitySourceReader.findByUserId(userId)).thenReturn(Optional.of(source));

        UserActivityResult result = userActivityApplication.findByUserId(userId);

        verify(writer).save(source); // 저장은 15건 그대로
        assertThat(result.comments()).hasSize(UserActivityResult.RECENT_LIMIT); // 응답은 10건
        assertThat(result.comments().get(0).content()).isEqualTo("댓글0"); // 앞(최신)부터
    }

    @Test
    @DisplayName("Mongo 문서가 있으면 그대로 돌려주고 원본은 읽지 않는다.")
    void givenDocument_whenFind_thenReturnsFromMongo() {
        UUID userId = UUID.randomUUID();
        UserActivityResult activity = emptyActivity(userId);
        when(userActivityReader.findByUserId(userId)).thenReturn(Optional.of(activity));

        UserActivityResult result = userActivityApplication.findByUserId(userId);

        assertThat(result).isEqualTo(activity);
        verifyNoInteractions(userActivitySourceReader);
    }

    @Test
    @DisplayName("없거나 탈퇴한 사용자면 Mongo 문서가 있어도 USER_NOT_FOUND 예외를 던진다.")
    void givenDeletedUser_whenFind_thenThrowsUserNotFound() {
        UUID userId = UUID.randomUUID();
        when(userService.getActiveUser(userId))
                .thenThrow(new MonewException(UserErrorType.USER_NOT_FOUND));

        assertThatThrownBy(() -> userActivityApplication.findByUserId(userId))
                .isInstanceOf(MonewException.class)
                .extracting("errorType")
                .isEqualTo(UserErrorType.USER_NOT_FOUND);
        verifyNoInteractions(userActivityReader);
    }

    private UserActivityResult emptyActivity(UUID userId) {
        return new UserActivityResult(
                userId,
                "woody@monew.com",
                "우디",
                Instant.now(),
                List.of(),
                List.of(),
                List.of(),
                List.of());
    }

    @Test
    @DisplayName("가입하면 빈 문서를 만든다.")
    void whenUserRegistered_thenCreate() {
        UUID userId = UUID.randomUUID();
        Instant createdAt = Instant.now();

        userActivityApplication.userRegistered(userId, "woody@monew.com", "우디", createdAt);

        verify(writer).create(userId, "woody@monew.com", "우디", createdAt);
    }

    @Test
    @DisplayName("닉네임이 바뀌면 문서 닉네임을 바꾼다.")
    void whenNicknameUpdated_thenUpdateNickname() {
        UUID userId = UUID.randomUUID();

        userActivityApplication.nicknameUpdated(userId, "버즈");

        verify(writer).updateNickname(userId, "버즈");
    }

    @Test
    @DisplayName("구독하면 구독 항목을 추가한다.")
    void whenSubscribed_thenAddSubscription() {
        UUID userId = UUID.randomUUID();
        SubscriptionActivityResult item =
                new SubscriptionActivityResult(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "축구",
                        List.of("손흥민"),
                        1L,
                        Instant.now());

        userActivityApplication.subscribed(userId, item);

        verify(writer).addSubscription(userId, item);
    }

    @Test
    @DisplayName("구독을 취소하면 구독 항목을 뺀다.")
    void whenUnsubscribed_thenRemoveSubscription() {
        UUID userId = UUID.randomUUID();
        UUID interestId = UUID.randomUUID();

        userActivityApplication.unsubscribed(userId, interestId);

        verify(writer).removeSubscription(userId, interestId);
    }

    @Test
    @DisplayName("관심사 키워드가 바뀌면 구독자 문서 키워드를 바꾼다.")
    void whenInterestKeywordsUpdated_thenUpdateKeywords() {
        UUID interestId = UUID.randomUUID();

        userActivityApplication.interestKeywordsUpdated(interestId, List.of("EPL"));

        verify(writer).updateInterestKeywords(interestId, List.of("EPL"));
    }

    @Test
    @DisplayName("관심사가 삭제되면 구독 목록에서 뺀다.")
    void whenInterestDeleted_thenRemoveInterest() {
        UUID interestId = UUID.randomUUID();

        userActivityApplication.interestDeleted(interestId);

        verify(writer).removeInterest(interestId);
    }

    @Test
    @DisplayName("댓글이 수정되면 댓글 내용을 바꾼다.")
    void whenCommentUpdated_thenUpdateContent() {
        UUID commentId = UUID.randomUUID();

        userActivityApplication.commentUpdated(commentId, "수정");

        verify(writer).updateCommentContent(commentId, "수정");
    }

    @Test
    @DisplayName("댓글이 삭제되면 댓글을 뺀다.")
    void whenCommentDeleted_thenRemoveComment() {
        UUID commentId = UUID.randomUUID();

        userActivityApplication.commentDeleted(commentId);

        verify(writer).removeComment(commentId);
    }

    @Test
    @DisplayName("기사를 보면 본 기사에 추가한다.")
    void whenArticleViewed_thenAddArticleView() {
        UUID userId = UUID.randomUUID();
        ArticleViewActivityResult item =
                new ArticleViewActivityResult(
                        UUID.randomUUID(),
                        userId,
                        Instant.now(),
                        UUID.randomUUID(),
                        ArticleSourceType.NAVER,
                        "https://a.com",
                        "기사",
                        Instant.now(),
                        "요약",
                        0L,
                        1L);

        userActivityApplication.articleViewed(userId, item);

        verify(writer).addArticleView(userId, item);
    }

    @Test
    @DisplayName("기사가 삭제되면 그 기사 항목을 뺀다.")
    void whenArticleDeleted_thenRemoveArticle() {
        UUID articleId = UUID.randomUUID();

        userActivityApplication.articleDeleted(articleId);

        verify(writer).removeArticle(articleId);
    }

    @Test
    @DisplayName("최근 본 기사의 조회 수·댓글 수는 원본의 현재 값으로 덮어쓴다.")
    void givenStaleCounts_whenFind_thenOverwrittenWithCurrent() {
        UUID userId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        ArticleViewActivityResult staleView = view(userId, articleId, 0L, 0L); // 본 시점 값
        when(userActivityReader.findByUserId(userId))
                .thenReturn(Optional.of(activityWithViews(userId, List.of(staleView))));
        when(articleStatReader.findStats(List.of(articleId)))
                .thenReturn(Map.of(articleId, new ArticleStat(5L, 2L))); // 현재 값

        UserActivityResult result = userActivityApplication.findByUserId(userId);

        ArticleViewActivityResult view = result.articleViews().get(0);
        assertThat(view.articleViewCount()).isEqualTo(5L);
        assertThat(view.articleCommentCount()).isEqualTo(2L);
        assertThat(view.articleTitle()).isEqualTo(staleView.articleTitle()); // 나머지는 그대로
    }

    @Test
    @DisplayName("원본에 숫자가 없는 기사는 문서의 값을 그대로 둔다.")
    void givenNoStat_whenFind_thenKeepsDocumentCounts() {
        UUID userId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        when(userActivityReader.findByUserId(userId))
                .thenReturn(
                        Optional.of(
                                activityWithViews(
                                        userId, List.of(view(userId, articleId, 3L, 1L)))));
        when(articleStatReader.findStats(List.of(articleId))).thenReturn(Map.of());

        UserActivityResult result = userActivityApplication.findByUserId(userId);

        assertThat(result.articleViews().get(0).articleViewCount()).isEqualTo(3L);
    }

    private UserActivityResult activityWithViews(
            UUID userId, List<ArticleViewActivityResult> views) {
        return new UserActivityResult(
                userId,
                "woody@monew.com",
                "우디",
                Instant.now(),
                List.of(),
                List.of(),
                List.of(),
                views);
    }

    private ArticleViewActivityResult view(
            UUID userId, UUID articleId, long viewCount, long commentCount) {
        return new ArticleViewActivityResult(
                UUID.randomUUID(),
                userId,
                Instant.now(),
                articleId,
                ArticleSourceType.NAVER,
                "https://a.com",
                "기사 제목",
                Instant.now(),
                "요약",
                commentCount,
                viewCount);
    }
}
