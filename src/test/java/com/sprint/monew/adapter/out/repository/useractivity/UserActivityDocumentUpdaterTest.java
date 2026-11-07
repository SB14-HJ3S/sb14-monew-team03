package com.sprint.monew.adapter.out.repository.useractivity;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.monew.adapter.out.repository.useractivity.document.ArticleViewItem;
import com.sprint.monew.adapter.out.repository.useractivity.document.CommentItem;
import com.sprint.monew.adapter.out.repository.useractivity.document.CommentLikeItem;
import com.sprint.monew.adapter.out.repository.useractivity.document.UserActivityDocument;
import com.sprint.monew.global.type.ArticleSourceType;
import com.sprint.monew.support.MongoIntegrationTest;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@MongoIntegrationTest
class UserActivityDocumentUpdaterTest {

    private static final Instant T = Instant.parse("2026-11-02T00:00:00Z");

    @Autowired private UserActivityMongoRepository repository;
    @Autowired private UserActivityDocumentUpdater updater;

    @AfterEach
    void tearDown() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("댓글을 21번 추가하면 문서에는 최신 20건까지 저장된다.")
    void givenTwentyOneComments_whenPush_thenKeepsLatestTwenty() {
        UUID userId = UUID.randomUUID();
        repository.save(UserActivityDocument.create(userId, "woody@monew.com", "우디", T));

        for (int i = 1; i <= 21; i++) {
            updater.pushComment(
                    userId,
                    new CommentItem(
                            UUID.randomUUID(),
                            UUID.randomUUID(),
                            "기사",
                            userId,
                            "우디",
                            "댓글" + i,
                            0L,
                            T.plusSeconds(i)));
        }

        UserActivityDocument found = repository.findById(userId).orElseThrow();
        assertThat(found.comments()).hasSize(UserActivityDocument.STORED_LIMIT);
        assertThat(found.comments().get(0).content()).isEqualTo("댓글21");
        assertThat(found.comments()).extracting(CommentItem::content).doesNotContain("댓글1");
    }

    @Test
    @DisplayName("좋아요를 21번 추가하면 문서에는 최신 20건까지 저장된다.")
    void givenTwentyOneLikes_whenPush_thenKeepsLatestTwenty() {
        UUID userId = UUID.randomUUID();
        repository.save(UserActivityDocument.create(userId, "woody@monew.com", "우디", T));

        for (int i = 1; i <= 21; i++) {
            updater.pushCommentLike(
                    userId,
                    new CommentLikeItem(
                            UUID.randomUUID(),
                            T.plusSeconds(i),
                            UUID.randomUUID(),
                            UUID.randomUUID(),
                            "기사",
                            UUID.randomUUID(),
                            "버즈",
                            "댓글" + i,
                            0L,
                            T));
        }

        UserActivityDocument found = repository.findById(userId).orElseThrow();
        assertThat(found.commentLikes()).hasSize(UserActivityDocument.STORED_LIMIT);
        assertThat(found.commentLikes().get(0).commentContent()).isEqualTo("댓글21");
        assertThat(found.commentLikes())
                .extracting(CommentLikeItem::commentContent)
                .doesNotContain("댓글1");
    }

    @Test
    @DisplayName("본 기사를 21번 추가하면 문서에는 최신 20건까지 저장된다.")
    void givenTwentyOneViews_whenPush_thenKeepsLatestTwenty() {
        UUID userId = UUID.randomUUID();
        repository.save(UserActivityDocument.create(userId, "woody@monew.com", "우디", T));

        for (int i = 1; i <= 21; i++) {
            updater.pushArticleView(
                    userId,
                    new ArticleViewItem(
                            UUID.randomUUID(),
                            userId,
                            T.plusSeconds(i),
                            UUID.randomUUID(),
                            ArticleSourceType.NAVER,
                            "https://a.com/" + i,
                            "기사" + i,
                            T,
                            "요약",
                            0L,
                            0L));
        }

        UserActivityDocument found = repository.findById(userId).orElseThrow();
        assertThat(found.articleViews()).hasSize(UserActivityDocument.STORED_LIMIT);
        assertThat(found.articleViews().get(0).articleTitle()).isEqualTo("기사21");
        assertThat(found.articleViews())
                .extracting(ArticleViewItem::articleTitle)
                .doesNotContain("기사1");
    }

    @Test
    @DisplayName("활동 내역 문서가 없으면 아무것도 만들지 않는다.")
    void givenNoDocument_whenPush_thenNothingCreated() {
        UUID userId = UUID.randomUUID();

        updater.pushComment(
                userId,
                new CommentItem(
                        UUID.randomUUID(), UUID.randomUUID(), "기사", userId, "우디", "댓글", 0L, T));

        assertThat(repository.findById(userId)).isEmpty();
    }
}
