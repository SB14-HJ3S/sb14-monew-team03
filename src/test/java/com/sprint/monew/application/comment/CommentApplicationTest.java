package com.sprint.monew.application.comment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.sprint.monew.application.comment.provided.dto.CommentResult;
import com.sprint.monew.application.comment.required.CommentLikeRepositoryPort;
import com.sprint.monew.application.comment.required.CommentRepositoryPort;
import com.sprint.monew.domain.article.entity.Article;
import com.sprint.monew.domain.comment.entity.Comment;
import com.sprint.monew.domain.user.entity.User;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.CommentErrorType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CommentApplicationTest {

    @Mock private CommentRepositoryPort commentRepository;
    @Mock private CommentLikeRepositoryPort commentLikeRepository;
    @InjectMocks private CommentApplication commentApplication;

    private final UUID commentId = UUID.randomUUID();
    private final UUID authorId = UUID.randomUUID();
    private final UUID articleId = UUID.randomUUID();
    private final Instant createdAt = Instant.parse("2026-10-21T00:00:00Z");

    // 단위 테스트에선 저장을 안 해서 id·createdAt이 null — 작성자는 mock으로, 댓글 id·생성일은 직접 넣는다
    private Comment savedComment(User author, Article article) {
        Comment comment = Comment.create(author, article, "수정 전");
        ReflectionTestUtils.setField(comment, "id", commentId);
        ReflectionTestUtils.setField(comment, "createdAt", createdAt);
        return comment;
    }

    private User author() {
        User author = mock(User.class);
        given(author.getId()).willReturn(authorId);
        return author;
    }

    @Nested
    @DisplayName("댓글 수정")
    class ChangeContent {

        @Test
        @DisplayName("작성자가 수정하면 내용이 바뀌고, 작성자 닉네임과 요청자 좋아요 여부가 담긴 결과를 돌려준다")
        void byAuthor() {
            User author = author();
            given(author.getNickname()).willReturn("작성자");
            Article article = mock(Article.class);
            given(article.getId()).willReturn(articleId);
            Comment comment = savedComment(author, article);
            given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
            given(commentLikeRepository.existsByCommentIdAndUserId(commentId, authorId))
                    .willReturn(true);

            CommentResult result = commentApplication.changeContent(commentId, authorId, "수정 후");

            assertThat(comment.getContent()).isEqualTo("수정 후");
            assertThat(result)
                    .isEqualTo(
                            new CommentResult(
                                    commentId, articleId, authorId, "작성자", "수정 후", 0L, true,
                                    createdAt));
        }

        @Test
        @DisplayName("작성자가 아니면 COMMENT_NOT_OWNER 예외가 나고 내용은 그대로다")
        void byNotAuthor() {
            Comment comment = savedComment(author(), mock(Article.class));
            given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));

            assertThatThrownBy(
                            () ->
                                    commentApplication.changeContent(
                                            commentId, UUID.randomUUID(), "수정 후"))
                    .isInstanceOf(MonewException.class)
                    .extracting("errorType")
                    .isEqualTo(CommentErrorType.COMMENT_NOT_OWNER);
            assertThat(comment.getContent()).isEqualTo("수정 전");
            verifyNoInteractions(commentLikeRepository);
        }

        @Test
        @DisplayName("삭제된 댓글이면 COMMENT_NOT_FOUND 예외가 난다")
        void deletedComment() {
            Comment comment = savedComment(mock(User.class), mock(Article.class));
            comment.softDelete();
            given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));

            assertThatThrownBy(() -> commentApplication.changeContent(commentId, authorId, "수정 후"))
                    .isInstanceOf(MonewException.class)
                    .extracting("errorType")
                    .isEqualTo(CommentErrorType.COMMENT_NOT_FOUND);
            assertThat(comment.getContent()).isEqualTo("수정 전");
            verifyNoInteractions(commentLikeRepository);
        }

        @Test
        @DisplayName("없는 댓글이면 COMMENT_NOT_FOUND 예외가 난다")
        void notExistComment() {
            given(commentRepository.findById(commentId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> commentApplication.changeContent(commentId, authorId, "수정 후"))
                    .isInstanceOf(MonewException.class)
                    .extracting("errorType")
                    .isEqualTo(CommentErrorType.COMMENT_NOT_FOUND);
            verifyNoInteractions(commentLikeRepository);
        }
    }
}
