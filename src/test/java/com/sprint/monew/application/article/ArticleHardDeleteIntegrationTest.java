package com.sprint.monew.application.article;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sprint.monew.application.article.provided.ArticleUseCase;
import com.sprint.monew.domain.article.entity.Article;
import com.sprint.monew.domain.article.entity.ArticleInterest;
import com.sprint.monew.domain.article.entity.ArticleViewStatus;
import com.sprint.monew.domain.comment.entity.Comment;
import com.sprint.monew.domain.comment.entity.CommentLike;
import com.sprint.monew.domain.interest.entity.Interest;
import com.sprint.monew.domain.notification.entity.Notification;
import com.sprint.monew.domain.user.entity.User;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.ArticleErrorType;
import com.sprint.monew.global.type.ArticleSourceType;
import com.sprint.monew.global.type.ResourceType;
import com.sprint.monew.support.IntegrationTest;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class ArticleHardDeleteIntegrationTest {

    @Autowired private ArticleUseCase articleUseCase;
    @Autowired private EntityManager em;
    @Autowired private JdbcTemplate jdbcTemplate;

    private Article savedArticle(String sourceUrl) {
        Article article =
                Article.create(
                        ArticleSourceType.NAVER, sourceUrl, "기사 제목", LocalDateTime.now(), "기사 요약");
        em.persist(article);
        return article;
    }

    private int countOf(String sql, UUID id) {
        return jdbcTemplate.queryForObject(sql, Integer.class, id);
    }

    @Test
    @DisplayName("뉴스 기사를 물리 삭제하면 댓글·좋아요·조회 기록·관심사 연결·댓글 좋아요 알림까지 함께 사라진다")
    void givenArticleWithRelatedData_whenHardDelete_thenRemoveAll() {
        User user = User.create("hard@test.com", "물리삭제", "password1!");
        em.persist(user);
        Interest interest = Interest.create("물리 삭제 관심사", List.of("경제"));
        em.persist(interest);
        Article article = savedArticle("https://news.example.com/hard");
        Comment comment = Comment.create(user, article, "댓글 내용");
        em.persist(comment);
        em.persist(CommentLike.create(user, comment));
        em.persist(ArticleViewStatus.create(user, article, LocalDateTime.now()));
        em.persist(ArticleInterest.create(article, interest));
        em.persist(Notification.create(user, ResourceType.COMMENT, comment.getId(), "좋아요 알림"));
        em.flush();
        em.clear();

        articleUseCase.hardDeleteArticle(article.getId());

        assertThat(countOf("SELECT count(*) FROM articles WHERE id = ?", article.getId())).isZero();
        assertThat(countOf("SELECT count(*) FROM comments WHERE article_id = ?", article.getId()))
                .isZero();
        assertThat(
                        countOf(
                                "SELECT count(*) FROM comment_likes WHERE comment_id = ?",
                                comment.getId()))
                .isZero();
        assertThat(
                        countOf(
                                "SELECT count(*) FROM article_view_statuses WHERE article_id = ?",
                                article.getId()))
                .isZero();
        assertThat(
                        countOf(
                                "SELECT count(*) FROM article_interests WHERE article_id = ?",
                                article.getId()))
                .isZero();
        assertThat(
                        countOf(
                                "SELECT count(*) FROM notifications WHERE resource_id = ?",
                                comment.getId()))
                .isZero();
    }

    @Test
    @DisplayName("논리 삭제된 뉴스 기사도 물리 삭제할 수 있다")
    void givenSoftDeletedArticle_whenHardDelete_thenRemove() {
        Article article = savedArticle("https://news.example.com/hard-soft");
        em.flush();
        articleUseCase.softDeleteArticle(article.getId());

        articleUseCase.hardDeleteArticle(article.getId());

        assertThat(countOf("SELECT count(*) FROM articles WHERE id = ?", article.getId())).isZero();
    }

    @Test
    @DisplayName("없는 뉴스 기사를 물리 삭제하면 ARTICLE_NOT_FOUND 예외가 난다")
    void givenNoArticle_whenHardDelete_thenThrowArticleNotFound() {
        assertThatThrownBy(() -> articleUseCase.hardDeleteArticle(UUID.randomUUID()))
                .isInstanceOf(MonewException.class)
                .extracting("errorType")
                .isEqualTo(ArticleErrorType.ARTICLE_NOT_FOUND);
    }
}
