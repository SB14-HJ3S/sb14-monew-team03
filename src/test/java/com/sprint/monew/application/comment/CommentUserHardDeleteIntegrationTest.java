package com.sprint.monew.application.comment;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.monew.adapter.out.repository.article.ArticleRepositoryAdapter;
import com.sprint.monew.adapter.out.repository.comment.CommentLikeRepositoryAdapter;
import com.sprint.monew.adapter.out.repository.comment.CommentRepositoryAdapter;
import com.sprint.monew.adapter.out.repository.user.UserRepositoryAdapter;
import com.sprint.monew.application.user.IUserService;
import com.sprint.monew.application.user.provided.UserUseCase;
import com.sprint.monew.domain.article.entity.Article;
import com.sprint.monew.domain.comment.entity.Comment;
import com.sprint.monew.domain.comment.entity.CommentLike;
import com.sprint.monew.domain.user.entity.User;
import com.sprint.monew.global.type.ArticleSourceType;
import com.sprint.monew.support.IntegrationTest;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

// 사용자 완전 삭제(사용자 쪽)부터 이벤트 → 댓글 리스너 → DB 삭제까지 실제로 이어지는지 확인
@IntegrationTest
class CommentUserHardDeleteIntegrationTest {

    @Autowired private UserUseCase userUseCase;
    @Autowired private IUserService userService;
    @Autowired private UserRepositoryAdapter userRepository;
    @Autowired private ArticleRepositoryAdapter articleRepository;
    @Autowired private CommentRepositoryAdapter commentRepository;
    @Autowired private CommentLikeRepositoryAdapter commentLikeRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private final List<UUID> userIds = new ArrayList<>();
    private final List<UUID> articleIds = new ArrayList<>();

    // 기사를 지우면 댓글·좋아요도 DB가 같이 지움
    @AfterEach
    void cleanUp() {
        articleIds.forEach(id -> jdbcTemplate.update("DELETE FROM articles WHERE id = ?", id));
        userIds.forEach(id -> jdbcTemplate.update("DELETE FROM users WHERE id = ?", id));
    }

    @Test
    @DisplayName("탈퇴자를 완전 삭제하면 탈퇴자 좋아요가 지워지고, 좋아요 수와 기사 댓글 수가 그만큼 준다")
    void hardDeleteUser() {
        User leaving = saveUser();
        User staying = saveUser();
        Article article = saveArticle();
        UUID stayingComment = saveComment(staying, article);
        like(leaving, stayingComment);
        like(staying, stayingComment);
        saveComment(leaving, article);
        saveComment(leaving, article);
        UUID alreadyDeleted = saveComment(leaving, article);
        softDelete(alreadyDeleted);
        // 살아 있는 댓글 3개 (논리 삭제된 댓글은 그때 이미 숫자에서 빠짐)
        setCommentCount(article, 3);
        userUseCase.softDeleteUser(leaving.getId());

        userUseCase.hardDeleteUser(leaving.getId());

        assertThat(likeRowsOf(stayingComment)).isEqualTo(1L);
        assertThat(likeCountOf(stayingComment)).isEqualTo(1L);
        assertThat(commentCountOf(article)).isEqualTo(1);
    }

    @Test
    @DisplayName("탈퇴자 여러 명을 한 번에 완전 삭제해도 각자 누른 좋아요와 쓴 댓글만큼 숫자가 준다")
    void hardDeleteUsers() {
        User leaving = saveUser();
        User otherLeaving = saveUser();
        User staying = saveUser();
        Article article = saveArticle();
        Article otherArticle = saveArticle();
        UUID stayingComment = saveComment(staying, article);
        like(leaving, stayingComment);
        like(otherLeaving, stayingComment);
        like(staying, stayingComment);
        saveComment(leaving, article);
        saveComment(otherLeaving, otherArticle);
        saveComment(staying, otherArticle);
        setCommentCount(article, 2);
        setCommentCount(otherArticle, 2);
        userUseCase.softDeleteUser(leaving.getId());
        userUseCase.softDeleteUser(otherLeaving.getId());

        int deleted = userService.hardDeleteUsers(List.of(leaving.getId(), otherLeaving.getId()));

        assertThat(deleted).isEqualTo(2);
        assertThat(likeRowsOf(stayingComment)).isEqualTo(1L);
        assertThat(likeCountOf(stayingComment)).isEqualTo(1L);
        assertThat(commentCountOf(article)).isEqualTo(1);
        assertThat(commentCountOf(otherArticle)).isEqualTo(1);
    }

    private User saveUser() {
        User saved =
                userRepository.save(
                        User.create(UUID.randomUUID() + "@monew.com", "사용자", "password1!"));
        userIds.add(saved.getId());
        return saved;
    }

    private Article saveArticle() {
        Article saved =
                articleRepository.save(
                        Article.create(
                                ArticleSourceType.NAVER,
                                "https://news.example.com/" + UUID.randomUUID(),
                                "기사 제목",
                                LocalDateTime.now(),
                                "기사 요약"));
        articleIds.add(saved.getId());
        return saved;
    }

    private UUID saveComment(User author, Article article) {
        return commentRepository.save(Comment.create(author, article, "댓글 내용")).getId();
    }

    // 좋아요를 저장하고 좋아요 수도 +1 (좋아요 API와 같은 결과)
    // 이 테스트엔 트랜잭션이 없어서 UPDATE 쿼리 메서드 대신 SQL로 바꿈
    private void like(User liker, UUID commentId) {
        Comment comment = commentRepository.findById(commentId).orElseThrow();
        commentLikeRepository.save(CommentLike.create(liker, comment));
        jdbcTemplate.update(
                "UPDATE comments SET like_count = like_count + 1 WHERE id = ?", commentId);
    }

    private void softDelete(UUID commentId) {
        jdbcTemplate.update(
                "UPDATE comments SET deleted_at = ? WHERE id = ?",
                Timestamp.from(Instant.now()),
                commentId);
    }

    private void setCommentCount(Article article, int commentCount) {
        jdbcTemplate.update(
                "UPDATE articles SET comment_count = ? WHERE id = ?",
                commentCount,
                article.getId());
    }

    // 사용자가 지워진 좋아요(user_id가 빈 줄)도 같이 셈
    private long likeRowsOf(UUID commentId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM comment_likes WHERE comment_id = ?", Long.class, commentId);
    }

    private long likeCountOf(UUID commentId) {
        return jdbcTemplate.queryForObject(
                "SELECT like_count FROM comments WHERE id = ?", Long.class, commentId);
    }

    private int commentCountOf(Article article) {
        return jdbcTemplate.queryForObject(
                "SELECT comment_count FROM articles WHERE id = ?", Integer.class, article.getId());
    }
}
