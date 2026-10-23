package com.sprint.monew.application.comment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.sprint.monew.application.comment.provided.dto.CommentLikeResult;
import com.sprint.monew.application.comment.provided.dto.CommentResult;
import com.sprint.monew.application.comment.required.CommentEventPublisherPort;
import com.sprint.monew.application.comment.required.CommentLikeRepositoryPort;
import com.sprint.monew.application.comment.required.CommentRepositoryPort;
import com.sprint.monew.application.user.provided.UserUseCase;
import com.sprint.monew.domain.article.entity.Article;
import com.sprint.monew.domain.comment.entity.Comment;
import com.sprint.monew.domain.comment.entity.CommentLike;
import com.sprint.monew.domain.comment.event.CommentLikedEvent;
import com.sprint.monew.domain.user.entity.User;
import com.sprint.monew.global.context.RequestIdContext;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.CommentErrorType;
import com.sprint.monew.global.exception.errortype.UserErrorType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
    @Mock private UserUseCase userUseCase;
    @Mock private CommentEventPublisherPort commentEventPublisher;
    @InjectMocks private CommentApplication commentApplication;

    private final UUID commentId = UUID.randomUUID();
    private final UUID authorId = UUID.randomUUID();
    private final UUID articleId = UUID.randomUUID();
    private final Instant createdAt = Instant.parse("2026-10-21T00:00:00Z");

    // 단위 테스트에선 인터셉터가 안 돌아서 요청자 ID를 직접 넣는다 — 다른 테스트에 남지 않게 지운다
    @AfterEach
    void clearRequestUserId() {
        RequestIdContext.clear();
    }

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

        @BeforeEach
        void setRequestUserId() {
            RequestIdContext.set(authorId);
        }

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

            CommentResult result = commentApplication.changeContent(commentId, "수정 후");

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
            RequestIdContext.set(UUID.randomUUID());

            assertThatThrownBy(() -> commentApplication.changeContent(commentId, "수정 후"))
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

            assertThatThrownBy(() -> commentApplication.changeContent(commentId, "수정 후"))
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

            assertThatThrownBy(() -> commentApplication.changeContent(commentId, "수정 후"))
                    .isInstanceOf(MonewException.class)
                    .extracting("errorType")
                    .isEqualTo(CommentErrorType.COMMENT_NOT_FOUND);
            verifyNoInteractions(commentLikeRepository);
        }
    }

    @Nested
    @DisplayName("댓글 좋아요 취소")
    class Unlike {

        private final UUID requestUserId = UUID.randomUUID();

        @BeforeEach
        void setRequestUserId() {
            RequestIdContext.set(requestUserId);
        }

        @Test
        @DisplayName("좋아요를 누른 사람이 취소하면 좋아요가 삭제되고 댓글 좋아요 수가 1 줄어든다")
        void success() {
            Comment comment = savedComment(mock(User.class), mock(Article.class));
            comment.increaseLikeCount();
            CommentLike commentLike = CommentLike.create(mock(User.class), comment);
            given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
            given(commentLikeRepository.findByCommentIdAndUserId(commentId, requestUserId))
                    .willReturn(Optional.of(commentLike));

            commentApplication.unlike(commentId);

            verify(commentLikeRepository).delete(commentLike);
            assertThat(comment.getLikeCount()).isZero();
        }

        @Test
        @DisplayName("좋아요를 누르지 않았으면 COMMENT_LIKE_NOT_FOUND 예외가 나고 좋아요 수는 그대로다")
        void notLiked() {
            Comment comment = savedComment(mock(User.class), mock(Article.class));
            comment.increaseLikeCount();
            given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
            given(commentLikeRepository.findByCommentIdAndUserId(commentId, requestUserId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> commentApplication.unlike(commentId))
                    .isInstanceOf(MonewException.class)
                    .extracting("errorType")
                    .isEqualTo(CommentErrorType.COMMENT_LIKE_NOT_FOUND);
            assertThat(comment.getLikeCount()).isEqualTo(1L);
            verify(commentLikeRepository, never()).delete(any());
        }

        @Test
        @DisplayName("삭제된 댓글이면 COMMENT_NOT_FOUND 예외가 난다")
        void deletedComment() {
            Comment comment = savedComment(mock(User.class), mock(Article.class));
            comment.softDelete();
            given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));

            assertThatThrownBy(() -> commentApplication.unlike(commentId))
                    .isInstanceOf(MonewException.class)
                    .extracting("errorType")
                    .isEqualTo(CommentErrorType.COMMENT_NOT_FOUND);
            verifyNoInteractions(commentLikeRepository);
        }

        @Test
        @DisplayName("없는 댓글이면 COMMENT_NOT_FOUND 예외가 난다")
        void notExistComment() {
            given(commentRepository.findById(commentId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> commentApplication.unlike(commentId))
                    .isInstanceOf(MonewException.class)
                    .extracting("errorType")
                    .isEqualTo(CommentErrorType.COMMENT_NOT_FOUND);
            verifyNoInteractions(commentLikeRepository);
        }
    }

    @Nested
    @DisplayName("댓글 좋아요")
    class Like {

        private final UUID requestUserId = UUID.randomUUID();

        @BeforeEach
        void setRequestUserId() {
            RequestIdContext.set(requestUserId);
        }

        private User liker() {
            User liker = mock(User.class);
            given(liker.getId()).willReturn(requestUserId);
            given(liker.getNickname()).willReturn("좋아요한 사람");
            return liker;
        }

        @Test
        @DisplayName("좋아요하면 저장되고 좋아요 수가 1 늘며, 댓글 작성자 알림용 이벤트를 발행하고 결과를 돌려준다")
        void success() {
            User author = author();
            given(author.getNickname()).willReturn("작성자");
            Article article = mock(Article.class);
            given(article.getId()).willReturn(articleId);
            Comment comment = savedComment(author, article);
            User liker = liker();
            given(userUseCase.getActiveUser(requestUserId)).willReturn(liker);
            given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
            given(commentLikeRepository.existsByCommentIdAndUserId(commentId, requestUserId))
                    .willReturn(false);
            given(commentLikeRepository.save(any(CommentLike.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            CommentLikeResult result = commentApplication.like(commentId);

            verify(commentLikeRepository).save(any(CommentLike.class));
            assertThat(comment.getLikeCount()).isEqualTo(1L);
            verify(commentEventPublisher)
                    .publish(new CommentLikedEvent(commentId, authorId, requestUserId, "좋아요한 사람"));
            assertThat(result)
                    .isEqualTo(
                            new CommentLikeResult(
                                    null,
                                    requestUserId,
                                    null,
                                    commentId,
                                    articleId,
                                    authorId,
                                    "작성자",
                                    "수정 전",
                                    1L,
                                    createdAt));
        }

        @Test
        @DisplayName("이미 좋아요한 댓글이면 COMMENT_LIKE_DUPLICATED 예외가 나고 저장·알림이 없다")
        void duplicated() {
            Comment comment = savedComment(mock(User.class), mock(Article.class));
            given(userUseCase.getActiveUser(requestUserId)).willReturn(mock(User.class));
            given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
            given(commentLikeRepository.existsByCommentIdAndUserId(commentId, requestUserId))
                    .willReturn(true);

            assertThatThrownBy(() -> commentApplication.like(commentId))
                    .isInstanceOf(MonewException.class)
                    .extracting("errorType")
                    .isEqualTo(CommentErrorType.COMMENT_LIKE_DUPLICATED);
            assertThat(comment.getLikeCount()).isZero();
            verify(commentLikeRepository, never()).save(any());
            verifyNoInteractions(commentEventPublisher);
        }

        @Test
        @DisplayName("없거나 탈퇴한 사용자면 USER_NOT_FOUND 예외가 나고 댓글·좋아요를 건드리지 않는다")
        void notExistUser() {
            given(userUseCase.getActiveUser(requestUserId))
                    .willThrow(new MonewException(UserErrorType.USER_NOT_FOUND));

            assertThatThrownBy(() -> commentApplication.like(commentId))
                    .isInstanceOf(MonewException.class)
                    .extracting("errorType")
                    .isEqualTo(UserErrorType.USER_NOT_FOUND);
            verifyNoInteractions(commentRepository, commentLikeRepository, commentEventPublisher);
        }

        @Test
        @DisplayName("삭제된 댓글이면 COMMENT_NOT_FOUND 예외가 나고 저장·알림이 없다")
        void deletedComment() {
            Comment comment = savedComment(mock(User.class), mock(Article.class));
            comment.softDelete();
            given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));

            assertThatThrownBy(() -> commentApplication.like(commentId))
                    .isInstanceOf(MonewException.class)
                    .extracting("errorType")
                    .isEqualTo(CommentErrorType.COMMENT_NOT_FOUND);
            verifyNoInteractions(commentLikeRepository, commentEventPublisher);
        }

        @Test
        @DisplayName("없는 댓글이면 COMMENT_NOT_FOUND 예외가 나고 저장·알림이 없다")
        void notExistComment() {
            given(commentRepository.findById(commentId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> commentApplication.like(commentId))
                    .isInstanceOf(MonewException.class)
                    .extracting("errorType")
                    .isEqualTo(CommentErrorType.COMMENT_NOT_FOUND);
            verifyNoInteractions(commentLikeRepository, commentEventPublisher);
        }
    }
}
