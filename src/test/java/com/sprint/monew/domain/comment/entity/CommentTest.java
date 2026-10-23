package com.sprint.monew.domain.comment.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.sprint.monew.domain.article.entity.Article;
import com.sprint.monew.domain.user.entity.User;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.CommentErrorType;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CommentTest {

    // 단위 테스트에선 저장을 안 해서 id가 null — 작성자 비교를 위해 User는 mock으로 id를 준다
    private final UUID authorId = UUID.randomUUID();
    private User author;
    private Article article;

    @BeforeEach
    void setUp() {
        author = mock(User.class);
        given(author.getId()).willReturn(authorId);
        article = mock(Article.class);
    }

    @Test
    @DisplayName("댓글을 만들면 작성자·기사·내용이 들어가고 좋아요 수는 0, 삭제되지 않은 상태다")
    void create() {
        Comment comment = Comment.create(author, article, "댓글 내용");

        assertThat(comment.getUser()).isEqualTo(author);
        assertThat(comment.getArticle()).isEqualTo(article);
        assertThat(comment.getContent()).isEqualTo("댓글 내용");
        assertThat(comment.getLikeCount()).isZero();
        assertThat(comment.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("작성자는 댓글 내용을 수정할 수 있다")
    void changeContent_byAuthor() {
        Comment comment = Comment.create(author, article, "수정 전");

        comment.changeContent(authorId, "수정 후");

        assertThat(comment.getContent()).isEqualTo("수정 후");
    }

    @Test
    @DisplayName("작성자가 아닌 사용자가 수정하면 COMMENT_NOT_OWNER 예외가 나고 내용은 그대로다")
    void changeContent_byNotAuthor() {
        Comment comment = Comment.create(author, article, "수정 전");

        assertThatThrownBy(() -> comment.changeContent(UUID.randomUUID(), "수정 후"))
                .isInstanceOf(MonewException.class)
                .extracting("errorType")
                .isEqualTo(CommentErrorType.COMMENT_NOT_OWNER);
        assertThat(comment.getContent()).isEqualTo("수정 전");
    }

    @Test
    @DisplayName("삭제된 댓글을 수정하면 COMMENT_NOT_FOUND 예외가 난다")
    void changeContent_deletedComment() {
        Comment comment = Comment.create(author, article, "수정 전");
        comment.softDelete();

        assertThatThrownBy(() -> comment.changeContent(authorId, "수정 후"))
                .isInstanceOf(MonewException.class)
                .extracting("errorType")
                .isEqualTo(CommentErrorType.COMMENT_NOT_FOUND);
        assertThat(comment.getContent()).isEqualTo("수정 전");
    }

    @Test
    @DisplayName("작성자 id와 같으면 작성자로, 다르면 작성자가 아닌 것으로 판단한다")
    void isAuthor() {
        Comment comment = Comment.create(author, article, "댓글 내용");

        assertThat(comment.isAuthor(authorId)).isTrue();
        assertThat(comment.isAuthor(UUID.randomUUID())).isFalse();
    }

    @Test
    @DisplayName("논리 삭제하면 deletedAt이 채워진다")
    void softDelete() {
        Comment comment = Comment.create(author, article, "댓글 내용");

        comment.softDelete();

        assertThat(comment.isDeleted()).isTrue();
        assertThat(comment.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("좋아요 수를 늘리고 줄일 수 있다")
    void increaseAndDecreaseLikeCount() {
        Comment comment = Comment.create(author, article, "댓글 내용");

        comment.increaseLikeCount();
        comment.increaseLikeCount();
        comment.decreaseLikeCount();

        assertThat(comment.getLikeCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("좋아요 수가 0일 때 줄여도 0 미만으로 내려가지 않는다")
    void decreaseLikeCount_notBelowZero() {
        Comment comment = Comment.create(author, article, "댓글 내용");

        comment.decreaseLikeCount();

        assertThat(comment.getLikeCount()).isZero();
    }
}
