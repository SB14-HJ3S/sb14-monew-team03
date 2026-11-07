package com.sprint.monew.adapter.out.article;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sprint.monew.adapter.out.comment.CommentRepositoryAdapter;
import com.sprint.monew.adapter.out.user.UserRepositoryAdapter;
import com.sprint.monew.domain.article.entity.Article;
import com.sprint.monew.domain.comment.entity.Comment;
import com.sprint.monew.domain.user.entity.User;
import com.sprint.monew.global.type.ArticleSourceType;
import com.sprint.monew.support.IntegrationTest;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class ArticleRepositoryAdapterTest {

    @Autowired private ArticleRepositoryAdapter articleRepository;
    @Autowired private EntityManager em;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("기사 발행일·생성 시각·ID 순서의 복합 인덱스가 DB에 생성된다")
    void givenArticleSchema_whenInspectIndexes_thenPublishedIndexExists() {
        String indexDefinition =
                jdbcTemplate.queryForObject(
                        """
                        SELECT indexdef
                        FROM pg_indexes
                        WHERE schemaname = current_schema()
                          AND tablename = 'articles'
                          AND indexname = 'idx_articles_published'
                        """,
                        String.class);

        assertThat(indexDefinition).contains("(published_at, created_at, id)");
    }

    private UUID savedArticleId() {
        return articleRepository
                .save(
                        Article.create(
                                ArticleSourceType.NAVER,
                                "https://news.example.com/comment-count",
                                "기사 제목",
                                LocalDateTime.now(),
                                "기사 요약"))
                .getId();
    }

    private int commentCountOf(UUID articleId) {
        return articleRepository.findById(articleId).orElseThrow().getCommentCount();
    }

    @Test
    @DisplayName("댓글 수를 늘리면 DB의 댓글 수가 1 늘고, 바뀐 줄 수 1을 돌려준다")
    void givenExistingArticle_whenIncreaseCommentCount_thenIncrementAndReturnOne() {
        // given
        UUID articleId = savedArticleId();

        // when
        int updated = articleRepository.increaseCommentCount(articleId);

        // then
        assertThat(updated).isEqualTo(1);
        assertThat(commentCountOf(articleId)).isEqualTo(1);
    }

    @Test
    @DisplayName("없는 기사면 바뀐 줄 수 0을 돌려준다")
    void givenMissingArticle_whenIncreaseCommentCount_thenReturnZero() {
        // given
        UUID articleId = UUID.randomUUID();

        // when
        int updated = articleRepository.increaseCommentCount(articleId);

        // then
        assertThat(updated).isZero();
    }

    @Test
    @DisplayName("댓글 수가 1 이상이면 DB의 댓글 수가 1 줄고, 바뀐 줄 수 1을 돌려준다")
    void givenPositiveCommentCount_whenDecreaseCommentCount_thenDecrementAndReturnOne() {
        // given
        UUID articleId = savedArticleId();
        articleRepository.increaseCommentCount(articleId);

        // when
        int updated = articleRepository.decreaseCommentCount(articleId);

        // then
        assertThat(updated).isEqualTo(1);
        assertThat(commentCountOf(articleId)).isZero();
    }

    @Test
    @DisplayName("댓글 수가 0이면 줄여도 0 그대로고, 바뀐 줄 수 0을 돌려준다")
    void givenZeroCommentCount_whenDecreaseCommentCount_thenKeepZeroAndReturnZero() {
        // given
        UUID articleId = savedArticleId();

        // when
        int updated = articleRepository.decreaseCommentCount(articleId);

        // then
        assertThat(updated).isZero();
        assertThat(commentCountOf(articleId)).isZero();
    }

    @Test
    @DisplayName("댓글 수를 음수로 바꾸려고 하면 DB가 거부한다")
    void givenExistingArticle_whenSetNegativeCommentCount_thenReject() {
        // given
        UUID articleId = savedArticleId();
        em.flush();

        // when & then
        assertThatThrownBy(
                        () ->
                                jdbcTemplate.update(
                                        "UPDATE articles SET comment_count = -1 WHERE id = ?",
                                        articleId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Nested
    @DisplayName("사용자 완전 삭제 전 기사 댓글 수 줄이기")
    class DecreaseCommentCountsByUserIds {

        @Autowired private UserRepositoryAdapter userRepository;
        @Autowired private CommentRepositoryAdapter commentRepository;

        private User savedUser() {
            return userRepository.save(
                    User.create(UUID.randomUUID() + "@monew.com", "사용자", "password1!"));
        }

        private Article savedArticle(String sourceUrl) {
            return articleRepository.save(
                    Article.create(
                            ArticleSourceType.NAVER,
                            sourceUrl,
                            "기사 제목",
                            LocalDateTime.now(),
                            "기사 요약"));
        }

        private UUID savedComment(User author, Article article) {
            return commentRepository.save(Comment.create(author, article, "댓글 내용")).getId();
        }

        private void setCommentCount(Article article, int commentCount) {
            em.flush();
            jdbcTemplate.update(
                    "UPDATE articles SET comment_count = ? WHERE id = ?",
                    commentCount,
                    article.getId());
            em.clear();
        }

        @Test
        @DisplayName("탈퇴자들이 쓴 댓글 수만큼 기사마다 댓글 수를 줄이고, 이미 논리 삭제된 댓글은 세지 않는다")
        void givenLeavingUsersComments_whenDecrease_thenDecreaseByActiveCommentCount() {
            // given
            User leaving = savedUser();
            User otherLeaving = savedUser();
            User staying = savedUser();
            Article first = savedArticle("https://news.example.com/user-delete-first");
            Article second = savedArticle("https://news.example.com/user-delete-second");
            Article untouched = savedArticle("https://news.example.com/user-delete-untouched");
            // first: 탈퇴자 댓글 2개 + 남는 사람 1개 (논리 삭제된 탈퇴자 댓글은 이미 숫자에서 빠짐)
            savedComment(leaving, first);
            savedComment(leaving, first);
            savedComment(staying, first);
            UUID alreadyDeleted = savedComment(leaving, first);
            commentRepository.softDeleteIfActive(alreadyDeleted, Instant.now());
            // second: 다른 탈퇴자 1개 + 남는 사람 1개
            savedComment(otherLeaving, second);
            savedComment(staying, second);
            // untouched: 남는 사람 1개
            savedComment(staying, untouched);
            setCommentCount(first, 3);
            setCommentCount(second, 2);
            setCommentCount(untouched, 1);

            // when
            int updated =
                    articleRepository.decreaseCommentCountsByUserIds(
                            List.of(leaving.getId(), otherLeaving.getId()));

            // then
            assertThat(updated).isEqualTo(2);
            assertThat(commentCountOf(first.getId())).isEqualTo(1);
            assertThat(commentCountOf(second.getId())).isEqualTo(1);
            assertThat(commentCountOf(untouched.getId())).isEqualTo(1);
        }

        @Test
        @DisplayName("댓글 수가 실제 댓글보다 작아도 0 아래로 내려가지 않는다")
        void givenUndercountedArticle_whenDecrease_thenKeepNonNegative() {
            // given
            User leaving = savedUser();
            Article article = savedArticle("https://news.example.com/user-delete-zero");
            savedComment(leaving, article);
            savedComment(leaving, article);
            setCommentCount(article, 1);

            // when
            articleRepository.decreaseCommentCountsByUserIds(List.of(leaving.getId()));

            // then
            assertThat(commentCountOf(article.getId())).isZero();
        }
    }

    @Test
    @DisplayName("살아 있는 뉴스 기사를 논리 삭제하면 삭제 시각이 들어가고, 바뀐 줄 수 1을 돌려준다")
    void givenActiveArticle_whenSoftDeleteIfActive_thenSetDeletedAtAndReturnOne() {
        UUID articleId = savedArticleId();
        Instant deletedAt = Instant.parse("2026-11-03T00:00:00Z");

        int updated = articleRepository.softDeleteIfActive(articleId, deletedAt);

        assertThat(updated).isEqualTo(1);
        assertThat(articleRepository.findById(articleId).orElseThrow().getDeletedAt())
                .isEqualTo(deletedAt);
    }

    @Test
    @DisplayName("이미 논리 삭제된 뉴스 기사면 삭제 시각을 바꾸지 않고, 바뀐 줄 수 0을 돌려준다")
    void givenDeletedArticle_whenSoftDeleteIfActive_thenKeepDeletedAtAndReturnZero() {
        UUID articleId = savedArticleId();
        Instant firstDeletedAt = Instant.parse("2026-11-03T00:00:00Z");
        articleRepository.softDeleteIfActive(articleId, firstDeletedAt);

        int updated =
                articleRepository.softDeleteIfActive(articleId, firstDeletedAt.plusSeconds(60));

        assertThat(updated).isZero();
        assertThat(articleRepository.findById(articleId).orElseThrow().getDeletedAt())
                .isEqualTo(firstDeletedAt);
    }

    @Test
    @DisplayName("뉴스 기사를 물리 삭제하면 DB에서 사라지고, 지운 줄 수 1을 돌려준다")
    void givenArticle_whenHardDeleteById_thenRemoveAndReturnOne() {
        UUID articleId = savedArticleId();

        int deleted = articleRepository.hardDeleteById(articleId);

        assertThat(deleted).isEqualTo(1);
        assertThat(articleRepository.findById(articleId)).isEmpty();
    }

    @Test
    @DisplayName("없는 뉴스 기사면 지운 줄 수 0을 돌려준다")
    void givenNoArticle_whenHardDeleteById_thenReturnZero() {
        int deleted = articleRepository.hardDeleteById(UUID.randomUUID());

        assertThat(deleted).isZero();
    }
}
