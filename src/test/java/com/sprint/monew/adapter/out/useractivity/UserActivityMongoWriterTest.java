package com.sprint.monew.adapter.out.useractivity;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.monew.adapter.out.useractivity.document.ArticleViewItem;
import com.sprint.monew.adapter.out.useractivity.document.UserActivityDocument;
import com.sprint.monew.application.useractivity.provided.dto.ArticleViewActivityResult;
import com.sprint.monew.application.useractivity.provided.dto.CommentActivityResult;
import com.sprint.monew.application.useractivity.provided.dto.CommentLikeActivityResult;
import com.sprint.monew.application.useractivity.provided.dto.SubscriptionActivityResult;
import com.sprint.monew.application.useractivity.provided.dto.UserActivityResult;
import com.sprint.monew.global.type.ArticleSourceType;
import com.sprint.monew.support.MongoIntegrationTest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@MongoIntegrationTest
class UserActivityMongoWriterTest {

    private static final Instant T = Instant.parse("2026-11-04T00:00:00Z");

    @Autowired private UserActivityMongoWriter writer;
    @Autowired private UserActivityMongoRepository repository;

    private final UUID woody = UUID.randomUUID();
    private final UUID buzz = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        writer.create(woody, "woody@monew.com", "우디", T);
        writer.create(buzz, "buzz@monew.com", "버즈", T);
    }

    @AfterEach
    void tearDown() {
        repository.deleteAll();
    }

    // ===== 사용자 =====

    @Test
    @DisplayName("가입하면 목록이 모두 빈 문서가 만들어진다.")
    void whenCreate_thenEmptyDocument() {
        UserActivityDocument doc = find(woody);

        assertThat(doc.nickname()).isEqualTo("우디");
        assertThat(doc.subscriptions()).isEmpty();
        assertThat(doc.comments()).isEmpty();
        assertThat(doc.commentLikes()).isEmpty();
        assertThat(doc.articleViews()).isEmpty();
    }

    @Test
    @DisplayName("가입 이벤트가 두 번 와도 기존 문서를 덮어쓰지 않는다.")
    void givenExisting_whenCreateAgain_thenKeepsData() {
        writer.addComment(woody, comment(UUID.randomUUID(), UUID.randomUUID(), woody));

        writer.create(woody, "other@monew.com", "다른이름", T);

        assertThat(find(woody).nickname()).isEqualTo("우디");
        assertThat(find(woody).comments()).hasSize(1);
    }

    @Test
    @DisplayName("닉네임을 바꾸면 본인 문서의 닉네임이 바뀐다.")
    void whenUpdateNickname_thenOwnNicknameChanged() {
        writer.updateNickname(buzz, "버즈라이트이어");

        assertThat(find(buzz).nickname()).isEqualTo("버즈라이트이어");
    }

    @Test
    @DisplayName("사용자들을 완전 삭제하면 그 사람들 문서와, 다른 사람 문서의 그 사람들 댓글 좋아요가 지워진다.")
    void givenUsers_whenDeleteAll_thenRemovedEverywhere() {
        UUID commentId = UUID.randomUUID();
        writer.addComment(buzz, comment(commentId, UUID.randomUUID(), buzz));
        writer.addCommentLike(woody, like(commentId, UUID.randomUUID(), buzz));

        writer.deleteAll(List.of(buzz));
        writer.removeCommentLikesOfCommentUsers(List.of(buzz));

        assertThat(repository.findById(buzz)).isEmpty();
        assertThat(find(woody).commentLikes()).isEmpty();
    }

    // ===== 구독 관심사 =====

    @Test
    @DisplayName("같은 관심사를 다시 구독해도 중복 없이 하나만 남는다.")
    void givenSubscribed_whenSubscribeAgain_thenNoDuplicate() {
        UUID interestId = UUID.randomUUID();
        writer.addSubscription(woody, subscription(interestId, List.of("손흥민")));

        writer.addSubscription(woody, subscription(interestId, List.of("손흥민")));

        assertThat(find(woody).subscriptions()).hasSize(1);
    }

    @Test
    @DisplayName("구독을 취소하면 그 관심사만 빠진다.")
    void givenSubscriptions_whenRemove_thenOnlyThatRemoved() {
        UUID soccer = UUID.randomUUID();
        UUID baseball = UUID.randomUUID();
        writer.addSubscription(woody, subscription(soccer, List.of("손흥민")));
        writer.addSubscription(woody, subscription(baseball, List.of("류현진")));

        writer.removeSubscription(woody, soccer);

        assertThat(find(woody).subscriptions()).hasSize(1);
        assertThat(find(woody).subscriptions().get(0).interestId()).isEqualTo(baseball);
    }

    @Test
    @DisplayName("관심사 키워드를 바꾸면 그 관심사를 구독한 모든 문서의 키워드만 바뀐다.")
    void givenSubscriptions_whenUpdateKeywords_thenUpdatedForAll() {
        UUID soccer = UUID.randomUUID();
        UUID baseball = UUID.randomUUID();
        writer.addSubscription(woody, subscription(soccer, List.of("손흥민")));
        writer.addSubscription(woody, subscription(baseball, List.of("류현진")));
        writer.addSubscription(buzz, subscription(soccer, List.of("손흥민")));

        writer.updateInterestKeywords(soccer, List.of("손흥민", "EPL"));

        assertThat(keywordsOf(woody, soccer)).containsExactly("손흥민", "EPL");
        assertThat(keywordsOf(buzz, soccer)).containsExactly("손흥민", "EPL");
        assertThat(keywordsOf(woody, baseball)).containsExactly("류현진"); // 다른 관심사는 그대로
    }

    @Test
    @DisplayName("관심사를 삭제하면 모든 문서의 구독 목록에서 빠진다.")
    void givenSubscriptions_whenRemoveInterest_thenRemovedForAll() {
        UUID interestId = UUID.randomUUID();
        writer.addSubscription(woody, subscription(interestId, List.of("손흥민")));
        writer.addSubscription(buzz, subscription(interestId, List.of("손흥민")));

        writer.removeInterest(interestId);

        assertThat(find(woody).subscriptions()).isEmpty();
        assertThat(find(buzz).subscriptions()).isEmpty();
    }

    // ===== 댓글 =====

    @Test
    @DisplayName("댓글 내용을 바꾸면 쓴 사람 문서와 좋아요 누른 사람 문서의 내용이 함께 바뀐다.")
    void givenCommentAndLike_whenUpdateContent_thenUpdatedEverywhere() {
        UUID commentId = UUID.randomUUID();
        writer.addComment(buzz, comment(commentId, UUID.randomUUID(), buzz));
        writer.addCommentLike(woody, like(commentId, UUID.randomUUID(), buzz));

        writer.updateCommentContent(commentId, "수정된 내용");

        assertThat(find(buzz).comments().get(0).content()).isEqualTo("수정된 내용");
        assertThat(find(woody).commentLikes().get(0).commentContent()).isEqualTo("수정된 내용");
    }

    @Test
    @DisplayName("댓글을 삭제하면 쓴 사람의 댓글 목록과 좋아요 누른 사람의 목록에서 모두 빠진다.")
    void givenCommentAndLike_whenRemoveComment_thenRemovedEverywhere() {
        UUID commentId = UUID.randomUUID();
        writer.addComment(buzz, comment(commentId, UUID.randomUUID(), buzz));
        writer.addCommentLike(woody, like(commentId, UUID.randomUUID(), buzz));

        writer.removeComment(commentId);

        assertThat(find(buzz).comments()).isEmpty();
        assertThat(find(woody).commentLikes()).isEmpty();
    }

    // ===== 좋아요 =====

    @Test
    @DisplayName("좋아요를 취소하면 내 문서에서 그 댓글 좋아요만 빠진다.")
    void givenLikes_whenRemoveLike_thenOnlyThatRemoved() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        writer.addCommentLike(woody, like(first, UUID.randomUUID(), buzz));
        writer.addCommentLike(woody, like(second, UUID.randomUUID(), buzz));

        writer.removeCommentLike(woody, first);

        assertThat(find(woody).commentLikes()).hasSize(1);
        assertThat(find(woody).commentLikes().get(0).commentId()).isEqualTo(second);
    }

    // ===== 기사 =====

    @Test
    @DisplayName("같은 기사를 다시 보면 중복 없이 맨 앞으로 올라온다.")
    void givenViewedArticle_whenViewAgain_thenMovedToTop() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        writer.addArticleView(woody, view(first, "첫 기사"));
        writer.addArticleView(woody, view(second, "두번째 기사"));

        writer.addArticleView(woody, view(first, "첫 기사"));

        assertThat(find(woody).articleViews())
                .extracting(ArticleViewItem::articleTitle)
                .containsExactly("첫 기사", "두번째 기사");
    }

    @Test
    @DisplayName("기사를 삭제하면 본 기사·댓글·좋아요한 댓글에서 그 기사 항목이 모두 빠진다.")
    void givenArticleActivities_whenRemoveArticle_thenRemovedEverywhere() {
        UUID articleId = UUID.randomUUID();
        UUID commentId = UUID.randomUUID();
        writer.addArticleView(woody, view(articleId, "기사"));
        writer.addComment(buzz, comment(commentId, articleId, buzz));
        writer.addCommentLike(woody, like(commentId, articleId, buzz));

        writer.removeArticle(articleId);

        assertThat(find(woody).articleViews()).isEmpty();
        assertThat(find(woody).commentLikes()).isEmpty();
        assertThat(find(buzz).comments()).isEmpty();
    }

    @Test
    @DisplayName("좋아요 수를 바꾸면 쓴 사람의 댓글과 좋아요 누른 사람들의 항목 숫자가 함께 바뀐다.")
    void givenCommentAndLike_whenUpdateLikeCount_thenUpdatedEverywhere() {
        UUID commentId = UUID.randomUUID();
        writer.addComment(buzz, comment(commentId, UUID.randomUUID(), buzz));
        writer.addCommentLike(woody, like(commentId, UUID.randomUUID(), buzz));

        writer.updateCommentLikeCount(commentId, 5L);

        assertThat(find(buzz).comments().get(0).likeCount()).isEqualTo(5L);
        assertThat(find(woody).commentLikes().get(0).commentLikeCount()).isEqualTo(5L);
    }

    @Test
    @DisplayName("활동 내역 전체를 저장하면 같은 내용으로 다시 꺼내진다.")
    void whenSave_thenSameResult() {
        UUID userId = UUID.randomUUID();
        UserActivityResult activity =
                new UserActivityResult(
                        userId,
                        "rex@monew.com",
                        "렉스",
                        T,
                        List.of(subscription(UUID.randomUUID(), List.of("손흥민"))),
                        List.of(comment(UUID.randomUUID(), UUID.randomUUID(), userId)),
                        List.of(),
                        List.of());

        writer.save(activity);

        assertThat(find(userId).toResult()).isEqualTo(activity);
    }

    // ===== 도우미 =====

    private UserActivityDocument find(UUID userId) {
        return repository.findById(userId).orElseThrow();
    }

    private List<String> keywordsOf(UUID userId, UUID interestId) {
        return find(userId).subscriptions().stream()
                .filter(s -> s.interestId().equals(interestId))
                .findFirst()
                .orElseThrow()
                .interestKeywords();
    }

    private CommentActivityResult comment(UUID commentId, UUID articleId, UUID userId) {
        return new CommentActivityResult(commentId, articleId, "기사", userId, "작성자", "내용", 0L, T);
    }

    private CommentLikeActivityResult like(UUID commentId, UUID articleId, UUID commentUserId) {
        return new CommentLikeActivityResult(
                UUID.randomUUID(),
                T,
                commentId,
                articleId,
                "기사",
                commentUserId,
                "작성자",
                "내용",
                0L,
                T);
    }

    private ArticleViewActivityResult view(UUID articleId, String title) {
        return new ArticleViewActivityResult(
                UUID.randomUUID(),
                woody,
                T,
                articleId,
                ArticleSourceType.NAVER,
                "https://a.com/" + articleId,
                title,
                T,
                "요약",
                0L,
                0L);
    }

    private SubscriptionActivityResult subscription(UUID interestId, List<String> keywords) {
        return new SubscriptionActivityResult(
                UUID.randomUUID(), interestId, "관심사", keywords, 1L, T);
    }
}
