package com.sprint.monew.adapter.out.comment;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.monew.adapter.out.article.ArticleRepositoryAdapter;
import com.sprint.monew.adapter.out.user.UserRepositoryAdapter;
import com.sprint.monew.domain.article.entity.Article;
import com.sprint.monew.domain.comment.entity.Comment;
import com.sprint.monew.domain.user.entity.User;
import com.sprint.monew.global.type.ArticleSourceType;
import com.sprint.monew.support.IntegrationTest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class CommentRepositoryAdapterTest {

    @Autowired private CommentRepositoryAdapter commentRepository;
    @Autowired private UserRepositoryAdapter userRepository;
    @Autowired private ArticleRepositoryAdapter articleRepository;

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
}
