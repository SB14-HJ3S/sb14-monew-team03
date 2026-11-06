package com.sprint.monew.adapter.out.comment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sprint.monew.adapter.out.article.ArticleRepositoryAdapter;
import com.sprint.monew.adapter.out.user.UserRepositoryAdapter;
import com.sprint.monew.application.comment.provided.dto.CommentQuery;
import com.sprint.monew.domain.article.entity.Article;
import com.sprint.monew.domain.comment.entity.Comment;
import com.sprint.monew.domain.comment.entity.CommentLike;
import com.sprint.monew.domain.user.entity.User;
import com.sprint.monew.global.pagination.SortDirection;
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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class CommentRepositoryAdapterTest {

    @Autowired private CommentRepositoryAdapter commentRepository;
    @Autowired private UserRepositoryAdapter userRepository;
    @Autowired private ArticleRepositoryAdapter articleRepository;
    @Autowired private EntityManager em;
    @Autowired private JdbcTemplate jdbcTemplate;

    // 좋아요 수가 likeCount인 댓글을 저장한다
    private UUID savedCommentId(long likeCount) {
        User user = userRepository.save(User.create("count@monew.com", "작성자", "password1!"));
        Article article =
                articleRepository.save(
                        Article.create(
                                ArticleSourceType.NAVER,
                                "https://news.example.com/count",
                                "기사 제목",
                                LocalDateTime.now(),
                                "기사 요약"));
        Comment comment = Comment.create(user, article, "댓글 내용");
        ReflectionTestUtils.setField(comment, "likeCount", likeCount);
        return commentRepository.save(comment).getId();
    }

    private long likeCountOf(UUID commentId) {
        return commentRepository.findById(commentId).orElseThrow().getLikeCount();
    }

    @Test
    @DisplayName("좋아요 수를 늘리면 DB의 좋아요 수가 1 늘고, 바뀐 줄 수 1을 돌려준다")
    void increaseLikeCount() {
        UUID commentId = savedCommentId(0L);

        int updated = commentRepository.increaseLikeCount(commentId);

        assertThat(updated).isEqualTo(1);
        assertThat(likeCountOf(commentId)).isEqualTo(1L);
    }

    @Test
    @DisplayName("좋아요 수를 줄이면 DB의 좋아요 수가 1 줄고, 바뀐 줄 수 1을 돌려준다")
    void decreaseLikeCount() {
        UUID commentId = savedCommentId(1L);

        int updated = commentRepository.decreaseLikeCount(commentId);

        assertThat(updated).isEqualTo(1);
        assertThat(likeCountOf(commentId)).isZero();
    }

    @Test
    @DisplayName("좋아요 수가 0이면 줄여도 0 그대로고, 바뀐 줄 수 0을 돌려준다")
    void decreaseLikeCountAtZero() {
        UUID commentId = savedCommentId(0L);

        int updated = commentRepository.decreaseLikeCount(commentId);

        assertThat(updated).isZero();
        assertThat(likeCountOf(commentId)).isZero();
    }

    @Test
    @DisplayName("살아 있는 댓글을 논리 삭제하면 삭제 시각이 들어가고, 바뀐 줄 수 1을 돌려준다")
    void softDeleteIfActive() {
        UUID commentId = savedCommentId(0L);
        Instant deletedAt = Instant.parse("2026-10-31T00:00:00Z");

        int updated = commentRepository.softDeleteIfActive(commentId, deletedAt);

        assertThat(updated).isEqualTo(1);
        assertThat(commentRepository.findById(commentId).orElseThrow().getDeletedAt())
                .isEqualTo(deletedAt);
    }

    @Test
    @DisplayName("이미 논리 삭제된 댓글이면 삭제 시각을 바꾸지 않고, 바뀐 줄 수 0을 돌려준다")
    void softDeleteIfActiveAlreadyDeleted() {
        UUID commentId = savedCommentId(0L);
        Instant firstDeletedAt = Instant.parse("2026-10-31T00:00:00Z");
        commentRepository.softDeleteIfActive(commentId, firstDeletedAt);

        int updated =
                commentRepository.softDeleteIfActive(commentId, firstDeletedAt.plusSeconds(60));

        assertThat(updated).isZero();
        assertThat(commentRepository.findById(commentId).orElseThrow().getDeletedAt())
                .isEqualTo(firstDeletedAt);
    }

    @Test
    @DisplayName("댓글을 물리 삭제하면 DB에서 사라지고, 지운 줄 수 1을 돌려준다")
    void hardDeleteById() {
        UUID commentId = savedCommentId(0L);

        int deleted = commentRepository.hardDeleteById(commentId);

        assertThat(deleted).isEqualTo(1);
        assertThat(commentRepository.findById(commentId)).isEmpty();
    }

    @Test
    @DisplayName("댓글을 읽은 뒤 다른 요청이 먼저 지웠으면, 예외 없이 지운 줄 수 0을 돌려준다")
    void hardDeleteByIdAlreadyDeleted() {
        UUID commentId = savedCommentId(0L);
        em.flush();
        em.clear();

        // 물리 삭제 요청이 댓글을 먼저 읽어 둔다
        commentRepository.findById(commentId).orElseThrow();

        // 그 사이 다른 물리 삭제 요청이 먼저 지운다
        jdbcTemplate.update("DELETE FROM comments WHERE id = ?", commentId);

        int deleted = commentRepository.hardDeleteById(commentId);

        assertThat(deleted).isZero();
    }

    @Test
    @DisplayName("댓글을 읽은 뒤 다른 요청이 삭제하고 좋아요를 눌러도, 내용 수정은 삭제 시각과 좋아요 수를 덮어쓰지 않는다")
    void changeContentKeepsColumnsChangedByOthers() {
        UUID commentId = savedCommentId(0L);
        em.flush();
        em.clear();

        // 수정 요청이 댓글을 먼저 읽어 둔다
        Comment comment = commentRepository.findById(commentId).orElseThrow();
        UUID authorId = comment.getUser().getId();

        // 그 사이 다른 요청이 논리 삭제하고 좋아요를 눌러 DB 값이 바뀐다
        jdbcTemplate.update(
                "UPDATE comments SET deleted_at = now(), like_count = 5 WHERE id = ?", commentId);

        // 먼저 읽어 둔 댓글로 수정한 내용이 저장된다
        comment.changeContent(authorId, "수정 후");
        em.flush();
        em.clear();

        Comment saved = commentRepository.findById(commentId).orElseThrow();
        assertThat(saved.getContent()).isEqualTo("수정 후");
        assertThat(saved.getDeletedAt()).isNotNull();
        assertThat(saved.getLikeCount()).isEqualTo(5L);
    }

    @Test
    @DisplayName("좋아요 수를 음수로 바꾸려고 하면 DB가 거부한다")
    void rejectNegativeLikeCount() {
        UUID commentId = savedCommentId(0L);
        em.flush();

        assertThatThrownBy(
                        () ->
                                jdbcTemplate.update(
                                        "UPDATE comments SET like_count = -1 WHERE id = ?",
                                        commentId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("기사의 댓글 ID를 찾으면 논리 삭제된 댓글도 포함하고, 다른 기사의 댓글은 빼고 돌려준다")
    void findIdsByArticleId() {
        User user = userRepository.save(User.create("ids@monew.com", "작성자", "password1!"));
        Article article = savedArticle("https://news.example.com/target");
        Article otherArticle = savedArticle("https://news.example.com/other");

        UUID activeId = commentRepository.save(Comment.create(user, article, "살아 있는 댓글")).getId();
        UUID deletedId = commentRepository.save(Comment.create(user, article, "지운 댓글")).getId();
        commentRepository.save(Comment.create(user, otherArticle, "다른 기사 댓글"));
        commentRepository.softDeleteIfActive(deletedId, Instant.now());

        // 기사를 물리 삭제하면 논리 삭제된 댓글도 같이 지워지니, 그 댓글의 알림도 지워야 한다
        assertThat(commentRepository.findIdsByArticleId(article.getId()))
                .containsExactlyInAnyOrder(activeId, deletedId);
    }

    @Nested
    @DisplayName("사용자 완전 삭제 전 좋아요 수 줄이기")
    class DecreaseLikeCountsByUserIds {

        @Autowired private CommentLikeRepositoryAdapter commentLikeRepository;

        private User savedUser() {
            return userRepository.save(
                    User.create(UUID.randomUUID() + "@monew.com", "사용자", "password1!"));
        }

        // likers가 좋아요를 누른 댓글을 저장하고, 좋아요 수도 그 인원수로 맞춘다
        private UUID savedLikedComment(User author, Article article, User... likers) {
            Comment comment = commentRepository.save(Comment.create(author, article, "댓글 내용"));
            for (User liker : likers) {
                commentLikeRepository.save(CommentLike.create(liker, comment));
            }
            em.flush();
            jdbcTemplate.update(
                    "UPDATE comments SET like_count = ? WHERE id = ?",
                    likers.length,
                    comment.getId());
            em.clear();
            return comment.getId();
        }

        @Test
        @DisplayName("탈퇴자들이 누른 좋아요 수만큼 댓글마다 좋아요 수를 줄이고, 탈퇴자가 안 누른 댓글은 그대로 둔다")
        void decreaseByLeavingUsersLikes() {
            User leaving = savedUser();
            User otherLeaving = savedUser();
            User staying = savedUser();
            Article article = savedArticle("https://news.example.com/user-delete-like");
            UUID likedByBoth = savedLikedComment(staying, article, leaving, otherLeaving, staying);
            UUID likedByOne = savedLikedComment(staying, article, leaving);
            UUID notLiked = savedLikedComment(staying, article, staying);

            int updated =
                    commentRepository.decreaseLikeCountsByUserIds(
                            List.of(leaving.getId(), otherLeaving.getId()));

            assertThat(updated).isEqualTo(2);
            assertThat(likeCountOf(likedByBoth)).isEqualTo(1);
            assertThat(likeCountOf(likedByOne)).isZero();
            assertThat(likeCountOf(notLiked)).isEqualTo(1);
        }

        @Test
        @DisplayName("좋아요 수가 실제 좋아요보다 작아도 0 아래로 내려가지 않는다")
        void notBelowZero() {
            User leaving = savedUser();
            User otherLeaving = savedUser();
            Article article = savedArticle("https://news.example.com/user-delete-like-zero");
            UUID commentId = savedLikedComment(leaving, article, leaving, otherLeaving);
            jdbcTemplate.update("UPDATE comments SET like_count = 1 WHERE id = ?", commentId);

            commentRepository.decreaseLikeCountsByUserIds(
                    List.of(leaving.getId(), otherLeaving.getId()));

            assertThat(likeCountOf(commentId)).isZero();
        }
    }

    private Article savedArticle(String sourceUrl) {
        return articleRepository.save(
                Article.create(
                        ArticleSourceType.NAVER, sourceUrl, "기사 제목", LocalDateTime.now(), "기사 요약"));
    }

    @Nested
    @DisplayName("댓글 목록 조회")
    class FindByQuery {

        private static final Instant T1 = Instant.parse("2026-10-01T00:00:00Z");
        private static final Instant T2 = Instant.parse("2026-10-02T00:00:00Z");
        private static final Instant T3 = Instant.parse("2026-10-03T00:00:00Z");
        private static final Instant T4 = Instant.parse("2026-10-04T00:00:00Z");
        private static final Instant T5 = Instant.parse("2026-10-05T00:00:00Z");

        private UUID articleId;
        private UUID first;
        private UUID second;
        private UUID third;
        private UUID otherArticleComment;

        @BeforeEach
        void setUp() {
            User user = userRepository.save(User.create("list@monew.com", "작성자", "password1!"));
            Article article = savedArticle("https://news.example.com/list");
            Article otherArticle = savedArticle("https://news.example.com/list-other");
            articleId = article.getId();

            // 좋아요 수: first 3, second 1, third 3 (first와 third는 좋아요 수가 같다)
            first = savedComment(user, article, T1, 3L);
            second = savedComment(user, article, T2, 1L);
            third = savedComment(user, article, T3, 3L);
            UUID deleted = savedComment(user, article, T4, 0L);
            otherArticleComment = savedComment(user, otherArticle, T5, 0L);
            commentRepository.softDeleteIfActive(deleted, Instant.now());
            em.clear();
        }

        // createdAt은 JPA Auditing이 저장 시각으로 채우므로, 저장 후 DB 값을 직접 바꿔 원하는 시각으로 맞춘다
        private UUID savedComment(User user, Article article, Instant createdAt, long likeCount) {
            UUID commentId = commentRepository.save(Comment.create(user, article, "댓글 내용")).getId();
            em.flush();
            jdbcTemplate.update(
                    "UPDATE comments SET created_at = ?, like_count = ? WHERE id = ?",
                    Timestamp.from(createdAt),
                    likeCount,
                    commentId);
            return commentId;
        }

        private List<UUID> idsOf(CommentQuery query) {
            return idsOf(query, null);
        }

        // cursorComment: 이전 페이지 마지막 댓글 (서비스가 커서 id로 찾아서 넘김)
        private List<UUID> idsOf(CommentQuery query, Comment cursorComment) {
            return commentRepository.findByQuery(query, cursorComment).stream()
                    .map(Comment::getId)
                    .toList();
        }

        private Comment commentOf(UUID commentId) {
            return commentRepository.findById(commentId).orElseThrow();
        }

        @Test
        @DisplayName("기사 ID를 주면 그 기사의 댓글만, 논리 삭제된 댓글은 빼고 돌려준다")
        void filterByArticle() {
            CommentQuery query =
                    new CommentQuery(
                            articleId,
                            CommentQuery.SortOrder.CREATED_AT,
                            SortDirection.DESC,
                            null,
                            null,
                            10);

            assertThat(idsOf(query)).containsExactly(third, second, first);
        }

        @Test
        @DisplayName("기사 ID가 없으면 모든 기사의 댓글을 돌려준다")
        void withoutArticle() {
            CommentQuery query =
                    new CommentQuery(
                            null,
                            CommentQuery.SortOrder.CREATED_AT,
                            SortDirection.DESC,
                            null,
                            null,
                            10);

            assertThat(idsOf(query)).containsExactly(otherArticleComment, third, second, first);
        }

        @Test
        @DisplayName("작성 시각 오름차순이면 오래된 댓글부터 돌려준다")
        void createdAtAsc() {
            CommentQuery query =
                    new CommentQuery(
                            articleId,
                            CommentQuery.SortOrder.CREATED_AT,
                            SortDirection.ASC,
                            null,
                            null,
                            10);

            assertThat(idsOf(query)).containsExactly(first, second, third);
        }

        @Test
        @DisplayName("좋아요 수 내림차순이면 좋아요가 많은 댓글부터, 같으면 최근 댓글부터 돌려준다")
        void likeCountDesc() {
            CommentQuery query =
                    new CommentQuery(
                            articleId,
                            CommentQuery.SortOrder.LIKE_COUNT,
                            SortDirection.DESC,
                            null,
                            null,
                            10);

            assertThat(idsOf(query)).containsExactly(third, first, second);
        }

        @Test
        @DisplayName("좋아요 수 오름차순이면 좋아요가 적은 댓글부터, 같으면 오래된 댓글부터 돌려준다")
        void likeCountAsc() {
            CommentQuery query =
                    new CommentQuery(
                            articleId,
                            CommentQuery.SortOrder.LIKE_COUNT,
                            SortDirection.ASC,
                            null,
                            null,
                            10);

            assertThat(idsOf(query)).containsExactly(second, first, third);
        }

        @Test
        @DisplayName("다음 페이지가 있는지 알 수 있게 limit보다 1개 더 가져온다")
        void fetchLimitPlusOne() {
            CommentQuery query =
                    new CommentQuery(
                            articleId,
                            CommentQuery.SortOrder.CREATED_AT,
                            SortDirection.DESC,
                            null,
                            null,
                            2);

            assertThat(idsOf(query)).hasSize(3);
        }

        @Test
        @DisplayName("작성 시각순에서 커서 댓글을 주면 그 다음 댓글부터 돌려준다")
        void createdAtCursor() {
            CommentQuery query =
                    new CommentQuery(
                            articleId,
                            CommentQuery.SortOrder.CREATED_AT,
                            SortDirection.DESC,
                            second.toString(),
                            T2,
                            10);

            assertThat(idsOf(query, commentOf(second))).containsExactly(first);
        }

        @Test
        @DisplayName("좋아요 수순에서 커서 댓글을 주면 좋아요 수가 같은 댓글도 빠짐없이 이어서 돌려준다")
        void likeCountCursor() {
            // third(좋아요 3, T3)까지 받았다. 같은 좋아요 3인 first가 다음 페이지에 나와야 한다
            CommentQuery query =
                    new CommentQuery(
                            articleId,
                            CommentQuery.SortOrder.LIKE_COUNT,
                            SortDirection.DESC,
                            third.toString(),
                            T3,
                            10);

            assertThat(idsOf(query, commentOf(third))).containsExactly(first, second);
        }

        @Test
        @DisplayName("좋아요 수와 작성 시각까지 같으면 id로 이어서 빠지는 댓글이 없다")
        void likeCountCursorSameCreatedAt() {
            UUID tieArticleId = savedTiedComments();
            CommentQuery firstPage =
                    new CommentQuery(
                            tieArticleId,
                            CommentQuery.SortOrder.LIKE_COUNT,
                            SortDirection.DESC,
                            null,
                            null,
                            1);
            List<UUID> fetched = idsOf(firstPage);
            CommentQuery nextPage =
                    new CommentQuery(
                            tieArticleId,
                            CommentQuery.SortOrder.LIKE_COUNT,
                            SortDirection.DESC,
                            fetched.get(0).toString(),
                            T1,
                            1);

            // 1페이지 마지막(fetched[0])에 이어서, 하나 더 가져왔던 댓글(fetched[1])이 나와야 한다
            assertThat(idsOf(nextPage, commentOf(fetched.get(0)))).containsExactly(fetched.get(1));
        }

        @Test
        @DisplayName("작성 시각까지 같으면 작성 시각순에서도 id로 이어서 빠지는 댓글이 없다")
        void createdAtCursorSameCreatedAt() {
            UUID tieArticleId = savedTiedComments();
            CommentQuery firstPage =
                    new CommentQuery(
                            tieArticleId,
                            CommentQuery.SortOrder.CREATED_AT,
                            SortDirection.ASC,
                            null,
                            null,
                            1);
            List<UUID> fetched = idsOf(firstPage);
            CommentQuery nextPage =
                    new CommentQuery(
                            tieArticleId,
                            CommentQuery.SortOrder.CREATED_AT,
                            SortDirection.ASC,
                            fetched.get(0).toString(),
                            T1,
                            1);

            assertThat(idsOf(nextPage, commentOf(fetched.get(0)))).containsExactly(fetched.get(1));
        }

        // 좋아요 수(2)와 작성 시각(T1)이 모두 같은 댓글 2개를 다른 기사에 저장하고 그 기사 ID를 돌려준다
        private UUID savedTiedComments() {
            User user = userRepository.save(User.create("tie@monew.com", "작성자", "password1!"));
            Article tieArticle = savedArticle("https://news.example.com/list-tie");
            savedComment(user, tieArticle, T1, 2L);
            savedComment(user, tieArticle, T1, 2L);
            em.clear();
            return tieArticle.getId();
        }

        @Test
        @DisplayName("전체 개수는 논리 삭제된 댓글과 다른 기사의 댓글을 빼고 센다")
        void countByQuery() {
            CommentQuery query =
                    new CommentQuery(
                            articleId,
                            CommentQuery.SortOrder.CREATED_AT,
                            SortDirection.DESC,
                            null,
                            null,
                            10);

            assertThat(commentRepository.countByQuery(query)).isEqualTo(3L);
        }
    }
}
