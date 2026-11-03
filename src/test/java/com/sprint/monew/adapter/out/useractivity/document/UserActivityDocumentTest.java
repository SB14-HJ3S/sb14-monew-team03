package com.sprint.monew.adapter.out.useractivity.document;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.monew.adapter.out.useractivity.UserActivityMongoRepository;
import com.sprint.monew.application.useractivity.provided.dto.UserActivityResult;
import com.sprint.monew.global.type.ArticleSourceType;
import com.sprint.monew.support.MongoIntegrationTest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@MongoIntegrationTest
class UserActivityDocumentTest {

    private static final Instant T = Instant.parse("2026-11-02T00:00:00Z");

    @Autowired private UserActivityMongoRepository repository;

    @AfterEach
    void tearDown() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("활동 내역 문서를 저장하고 조회하면 모든 항목이 그대로 결과 객체로 변환된다.")
    void givenDocument_whenSaveAndFind_thenSameResult() {
        // given
        UUID userId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        UserActivityDocument document =
                new UserActivityDocument(
                        userId,
                        "woody@monew.com",
                        "우디",
                        T,
                        List.of(
                                new SubscriptionItem(
                                        UUID.randomUUID(),
                                        UUID.randomUUID(),
                                        "축구",
                                        List.of("손흥민", "EPL"),
                                        3L,
                                        T)),
                        List.of(
                                new CommentItem(
                                        UUID.randomUUID(),
                                        articleId,
                                        "기사 제목",
                                        userId,
                                        "우디",
                                        "댓글 내용",
                                        2L,
                                        T)),
                        List.of(
                                new CommentLikeItem(
                                        UUID.randomUUID(),
                                        T,
                                        UUID.randomUUID(),
                                        articleId,
                                        "기사 제목",
                                        UUID.randomUUID(),
                                        "버즈",
                                        "버즈 댓글",
                                        5L,
                                        T)),
                        List.of(
                                new ArticleViewItem(
                                        UUID.randomUUID(),
                                        userId,
                                        T,
                                        articleId,
                                        ArticleSourceType.NAVER,
                                        "https://a.com/1",
                                        "기사 제목",
                                        T,
                                        "요약",
                                        1L,
                                        10L)));

        // when
        repository.save(document);
        UserActivityResult result = repository.findById(userId).orElseThrow().toResult();

        // then
        assertThat(result.nickname()).isEqualTo("우디");
        assertThat(result.subscriptions().get(0).interestKeywords()).containsExactly("손흥민", "EPL");
        assertThat(result.comments().get(0).content()).isEqualTo("댓글 내용");
        assertThat(result.commentLikes().get(0).commentUserNickname()).isEqualTo("버즈");
        assertThat(result.articleViews().get(0).source()).isEqualTo(ArticleSourceType.NAVER);
        assertThat(result).isEqualTo(document.toResult()); // 전체 값이 저장 전과 같은지
    }

    @Test
    @DisplayName("가입 직후 문서는 목록이 모두 비어 있다.")
    void whenCreate_thenEmptyLists() {
        UserActivityDocument created =
                UserActivityDocument.create(UUID.randomUUID(), "w@m.com", "우디", T);

        repository.save(created);
        UserActivityResult result = repository.findById(created.id()).orElseThrow().toResult();

        assertThat(result.subscriptions()).isEmpty();
        assertThat(result.comments()).isEmpty();
        assertThat(result.commentLikes()).isEmpty();
        assertThat(result.articleViews()).isEmpty();
    }
}
