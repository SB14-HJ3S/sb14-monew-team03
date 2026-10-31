package com.sprint.monew.adapter.out.useractivity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import com.sprint.monew.application.useractivity.provided.dto.ArticleViewActivityResult;
import com.sprint.monew.application.useractivity.provided.dto.CommentActivityResult;
import com.sprint.monew.application.useractivity.provided.dto.CommentLikeActivityResult;
import com.sprint.monew.application.useractivity.provided.dto.SubscriptionActivityResult;
import com.sprint.monew.domain.article.entity.Article;
import com.sprint.monew.domain.article.entity.ArticleViewStatus;
import com.sprint.monew.domain.comment.entity.Comment;
import com.sprint.monew.domain.comment.entity.CommentLike;
import com.sprint.monew.domain.interest.entity.Interest;
import com.sprint.monew.domain.user.entity.Subscription;
import com.sprint.monew.domain.user.entity.User;
import com.sprint.monew.global.type.ArticleSourceType;
import com.sprint.monew.support.IntegrationTest;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class UserActivityRdbAdapterTest {

    private static final Instant T = Instant.parse("2026-10-30T00:00:00Z");

    @Autowired UserActivityRdbAdapter adapter;
    @Autowired EntityManager em;
    @Autowired JdbcTemplate jdbcTemplate;

    User woody;
    User buzz;
    Article article;

    @BeforeEach
    void setUp() {
        woody = persist(User.create("woody@monew.com", "우디", "monew1!")); // 실제 생성 방식으로
        buzz = persist(User.create("buzz@monew.com", "버즈", "monew1!"));
        article =
                persist(
                        Article.create(
                                ArticleSourceType.NAVER,
                                "https://a.com/1",
                                "기사 제목",
                                LocalDateTime.of(2026, 10, 1, 9, 0),
                                "요약"));
    }

    @Test
    @DisplayName("없는 사용자면 빈 값을 반환한다.")
    void givenUnknownUser_whenFind_thenEmpty() {
        assertThat(adapter.findByUserId(UUID.randomUUID())).isEmpty();
    }

    @Test
    @DisplayName("탈퇴한 사용자면 빈 값을 반환한다.")
    void givenDeletedUser_whenFind_thenEmpty() {
        setColumn("users", "deleted_at", Timestamp.from(T), woody.getId());
        clear();

        assertThat(adapter.findByUserId(woody.getId())).isEmpty();
    }

    @Test
    @DisplayName("최근 작성한 댓글은 최신순 최대 10건이고, 삭제된 댓글은 제외한다.")
    void givenElevenComments_whenFind_thenLatestTenWithoutDeleted() {
        for (int i = 0; i < 11; i++) {
            Comment c = persist(Comment.create(woody, article, "댓글" + i));
            setColumn("comments", "created_at", Timestamp.from(T.plusSeconds(i)), c.getId());
        }
        Comment deleted = persist(Comment.create(woody, article, "삭제된 댓글"));
        setColumn("comments", "created_at", Timestamp.from(T.plusSeconds(100)), deleted.getId());
        setColumn("comments", "deleted_at", Timestamp.from(T), deleted.getId());
        clear();

        List<CommentActivityResult> comments =
                adapter.findByUserId(woody.getId()).orElseThrow().comments();

        assertThat(comments).hasSize(10);
        assertThat(comments)
                .extracting(CommentActivityResult::content)
                .startsWith("댓글10", "댓글9") // 최신순
                .doesNotContain("댓글0", "삭제된 댓글"); // 11번째와 삭제된 것 제외
        assertThat(comments.get(0).articleTitle()).isEqualTo("기사 제목");
    }

    @Test
    @DisplayName("좋아요 누른 댓글은 내가 누른 것만, 댓글 작성자 정보와 함께 반환한다.")
    void givenLikes_whenFind_thenOnlyMyLikesWithCommentAuthor() {
        Comment buzzComment = persist(Comment.create(buzz, article, "버즈 댓글"));
        persist(CommentLike.create(woody, buzzComment)); // 우디가 누름
        Comment woodyComment = persist(Comment.create(woody, article, "우디 댓글"));
        persist(CommentLike.create(buzz, woodyComment)); // 버즈가 누름 → 우디 활동 아님
        clear();

        List<CommentLikeActivityResult> likes =
                adapter.findByUserId(woody.getId()).orElseThrow().commentLikes();

        assertThat(likes).hasSize(1);
        assertThat(likes.get(0).commentContent()).isEqualTo("버즈 댓글");
        assertThat(likes.get(0).commentUserNickname()).isEqualTo("버즈");
    }

    @Test
    @DisplayName("본 기사는 마지막으로 본 순서로 반환한다.")
    void givenViews_whenFind_thenOrderByLastViewedAt() {
        Article other =
                persist(
                        Article.create(
                                ArticleSourceType.NAVER,
                                "https://a.com/2",
                                "다른 기사",
                                LocalDateTime.of(2026, 10, 2, 9, 0),
                                "요약"));
        persist(ArticleViewStatus.create(woody, article, LocalDateTime.of(2026, 10, 30, 9, 0)));
        persist(ArticleViewStatus.create(woody, other, LocalDateTime.of(2026, 10, 30, 10, 0)));
        clear();

        List<ArticleViewActivityResult> views =
                adapter.findByUserId(woody.getId()).orElseThrow().articleViews();

        assertThat(views)
                .extracting(ArticleViewActivityResult::articleTitle)
                .containsExactly("다른 기사", "기사 제목");
        assertThat(views.get(0).viewedBy()).isEqualTo(woody.getId());
    }

    @Test
    @DisplayName("구독한 관심사를 키워드와 함께 반환한다.")
    void givenSubscription_whenFind_thenWithKeywords() {
        Interest interest = persist(Interest.create("축구", List.of("손흥민", "EPL")));
        persist(Subscription.create(woody, interest));
        clear();

        List<SubscriptionActivityResult> subs =
                adapter.findByUserId(woody.getId()).orElseThrow().subscriptions();

        assertThat(subs).hasSize(1);
        assertThat(subs.get(0).interestName()).isEqualTo("축구");
        assertThat(subs.get(0).interestKeywords()).containsExactlyInAnyOrder("손흥민", "EPL");
    }

    // ===== 도우미 =====

    private <T> T persist(T entity) {
        em.persist(entity);
        em.flush();
        return entity;
    }

    // createdAt은 Auditing이 자동으로 채우므로, 순서 테스트를 위해 DB 값을 직접 바꾼다
    private void setColumn(String table, String column, Object value, UUID id) {
        jdbcTemplate.update("UPDATE " + table + " SET " + column + " = ? WHERE id = ?", value, id);
    }

    private void clear() {
        em.flush();
        em.clear();
    }
}
