package com.sprint.monew.adapter.out.comment;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sprint.monew.adapter.out.article.ArticleRepositoryAdapter;
import com.sprint.monew.adapter.out.user.UserRepositoryAdapter;
import com.sprint.monew.domain.article.entity.Article;
import com.sprint.monew.domain.comment.entity.Comment;
import com.sprint.monew.domain.comment.entity.CommentLike;
import com.sprint.monew.domain.user.entity.User;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.CommentErrorType;
import com.sprint.monew.global.type.ArticleSourceType;
import com.sprint.monew.support.IntegrationTest;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class CommentLikeRepositoryAdapterTest {

    @Autowired private CommentLikeRepositoryAdapter commentLikeRepository;
    @Autowired private CommentRepositoryAdapter commentRepository;
    @Autowired private UserRepositoryAdapter userRepository;
    @Autowired private ArticleRepositoryAdapter articleRepository;
    @Autowired private EntityManager entityManager;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("같은 사용자가 같은 댓글에 좋아요를 또 저장하면 COMMENT_LIKE_DUPLICATED 예외가 난다")
    void givenAlreadyLiked_whenSave_thenDuplicated() {
        User user = userRepository.save(User.create("like@monew.com", "좋아요", "password1!"));
        Article article =
                articleRepository.save(
                        Article.create(
                                ArticleSourceType.NAVER,
                                "https://news.example.com/1",
                                "기사 제목",
                                LocalDateTime.now(),
                                "기사 요약"));
        Comment comment = commentRepository.save(Comment.create(user, article, "댓글 내용"));
        commentLikeRepository.save(CommentLike.create(user, comment));

        assertThatThrownBy(() -> commentLikeRepository.save(CommentLike.create(user, comment)))
                .isInstanceOf(MonewException.class)
                .extracting("errorType")
                .isEqualTo(CommentErrorType.COMMENT_LIKE_DUPLICATED);
    }

    @Test
    @DisplayName("중복이 아닌 DB 제약 위반(좋아요할 댓글이 방금 지워짐)은 중복 좋아요로 바꾸지 않고 그대로 던진다")
    void givenDeletedComment_whenSave_thenNotDuplicated() {
        User user = userRepository.save(User.create("gone@monew.com", "좋아요", "password1!"));
        Article article =
                articleRepository.save(
                        Article.create(
                                ArticleSourceType.NAVER,
                                "https://news.example.com/2",
                                "기사 제목",
                                LocalDateTime.now(),
                                "기사 요약"));
        Comment comment = commentRepository.save(Comment.create(user, article, "댓글 내용"));
        entityManager.flush();
        // 좋아요 검사를 통과한 직후 댓글이 물리 삭제된 상황 — 저장하면 외래키 위반이 난다
        jdbcTemplate.update("DELETE FROM comments WHERE id = ?", comment.getId());

        assertThatThrownBy(() -> commentLikeRepository.save(CommentLike.create(user, comment)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
