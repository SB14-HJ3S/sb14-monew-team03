package com.sprint.monew.application.comment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.sprint.monew.application.article.ArticleService;
import com.sprint.monew.application.comment.provided.dto.CommentLikeResult;
import com.sprint.monew.application.comment.provided.dto.CommentQuery;
import com.sprint.monew.application.comment.provided.dto.CommentResult;
import com.sprint.monew.application.comment.required.CommentEventPublisherPort;
import com.sprint.monew.application.comment.required.CommentLikeRepositoryPort;
import com.sprint.monew.application.comment.required.CommentRepositoryPort;
import com.sprint.monew.application.notification.NotificationService;
import com.sprint.monew.application.user.IUserService;
import com.sprint.monew.domain.article.entity.Article;
import com.sprint.monew.domain.comment.entity.Comment;
import com.sprint.monew.domain.comment.entity.CommentLike;
import com.sprint.monew.domain.comment.event.CommentLikedEvent;
import com.sprint.monew.domain.user.entity.User;
import com.sprint.monew.global.context.RequestIdContext;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.ArticleErrorType;
import com.sprint.monew.global.exception.errortype.CommentErrorType;
import com.sprint.monew.global.exception.errortype.CommonErrorType;
import com.sprint.monew.global.exception.errortype.UserErrorType;
import com.sprint.monew.global.pagination.PageResult;
import com.sprint.monew.global.pagination.SortDirection;
import com.sprint.monew.global.type.ResourceType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CommentApplicationTest {

    @Mock private CommentRepositoryPort commentRepository;
    @Mock private CommentLikeRepositoryPort commentLikeRepository;
    @Mock private IUserService userService;
    @Mock private ArticleService articleService;
    @Mock private CommentEventPublisherPort commentEventPublisher;
    @Mock private NotificationService notificationService;
    private CommentApplication commentApplication;

    private final UUID commentId = UUID.randomUUID();
    private final UUID authorId = UUID.randomUUID();
    private final UUID articleId = UUID.randomUUID();
    private final Instant createdAt = Instant.parse("2026-10-21T00:00:00Z");

    // CommentServiceImpl은 진짜로 쓰고 DB 포트만 가짜로 둬서 Application과 Service를 같이 검증한다
    @BeforeEach
    void setUp() {
        commentApplication =
                new CommentApplication(
                        new CommentServiceImpl(commentRepository, commentLikeRepository),
                        userService,
                        articleService,
                        commentEventPublisher,
                        notificationService);
    }

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
        @DisplayName("좋아요를 누른 사람이 취소하면 좋아요가 삭제되고 DB에서 댓글 좋아요 수를 1 줄인다")
        void success() {
            Comment comment = savedComment(mock(User.class), mock(Article.class));
            CommentLike commentLike = CommentLike.create(mock(User.class), comment);
            given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
            given(commentLikeRepository.findByCommentIdAndUserId(commentId, requestUserId))
                    .willReturn(Optional.of(commentLike));

            commentApplication.unlike(commentId);

            verify(commentLikeRepository).delete(commentLike);
            verify(commentRepository).decreaseLikeCount(commentId);
        }

        @Test
        @DisplayName("좋아요를 누르지 않았으면 COMMENT_LIKE_NOT_FOUND 예외가 나고 좋아요 수는 그대로다")
        void notLiked() {
            Comment comment = savedComment(mock(User.class), mock(Article.class));
            given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
            given(commentLikeRepository.findByCommentIdAndUserId(commentId, requestUserId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> commentApplication.unlike(commentId))
                    .isInstanceOf(MonewException.class)
                    .extracting("errorType")
                    .isEqualTo(CommentErrorType.COMMENT_LIKE_NOT_FOUND);
            verify(commentRepository, never()).decreaseLikeCount(any());
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
        @DisplayName("좋아요하면 저장되고 알림용 이벤트를 발행한 뒤 DB에서 좋아요 수를 1 늘리고, 늘어난 좋아요 수가 담긴 결과를 돌려준다")
        void success() {
            User author = author();
            given(author.getNickname()).willReturn("작성자");
            Article article = mock(Article.class);
            given(article.getId()).willReturn(articleId);
            Comment comment = savedComment(author, article);
            // 좋아요 수를 올린 뒤 다시 읽는 댓글 — DB에서 늘어난 좋아요 수(1)를 담고 있다
            Comment likedComment = savedComment(author, article);
            ReflectionTestUtils.setField(likedComment, "likeCount", 1L);
            User liker = liker();
            given(userService.getActiveUser(requestUserId)).willReturn(liker);
            given(commentRepository.findById(commentId))
                    .willReturn(Optional.of(comment), Optional.of(likedComment));
            given(commentLikeRepository.existsByCommentIdAndUserId(commentId, requestUserId))
                    .willReturn(false);
            given(commentLikeRepository.save(any(CommentLike.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            CommentLikeResult result = commentApplication.like(commentId);

            verify(commentLikeRepository).save(any(CommentLike.class));
            // 좋아요 수를 올리면 영속성 컨텍스트가 비워지므로 이벤트가 먼저다
            InOrder inOrder = inOrder(commentEventPublisher, commentRepository);
            inOrder.verify(commentEventPublisher)
                    .publish(new CommentLikedEvent(commentId, authorId, requestUserId, "좋아요한 사람"));
            inOrder.verify(commentRepository).increaseLikeCount(commentId);
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
            // 중복 확인을 서비스가 사용자 객체의 id로 하므로 id를 채운다
            User liker = mock(User.class);
            given(liker.getId()).willReturn(requestUserId);
            given(userService.getActiveUser(requestUserId)).willReturn(liker);
            given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
            given(commentLikeRepository.existsByCommentIdAndUserId(commentId, requestUserId))
                    .willReturn(true);

            assertThatThrownBy(() -> commentApplication.like(commentId))
                    .isInstanceOf(MonewException.class)
                    .extracting("errorType")
                    .isEqualTo(CommentErrorType.COMMENT_LIKE_DUPLICATED);
            verify(commentRepository, never()).increaseLikeCount(any());
            verify(commentLikeRepository, never()).save(any());
            verifyNoInteractions(commentEventPublisher);
        }

        @Test
        @DisplayName("없거나 탈퇴한 사용자면 USER_NOT_FOUND 예외가 나고 댓글·좋아요를 건드리지 않는다")
        void notExistUser() {
            given(userService.getActiveUser(requestUserId))
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

    @Nested
    @DisplayName("댓글 등록")
    class Register {

        @Test
        @DisplayName("기사에 댓글을 등록하면 저장되고, 작성자 닉네임과 좋아요 0·내 좋아요 아님이 담긴 결과를 돌려준다")
        void success() {
            User author = author();
            given(author.getNickname()).willReturn("작성자");
            Article article = mock(Article.class);
            given(article.getId()).willReturn(articleId);
            given(userService.getActiveUser(authorId)).willReturn(author);
            given(articleService.findById(articleId)).willReturn(article);
            // 저장하면 DB가 id·생성일을 채워 주는 것처럼 직접 넣는다
            given(commentRepository.save(any(Comment.class)))
                    .willAnswer(
                            invocation -> {
                                Comment saved = invocation.getArgument(0);
                                ReflectionTestUtils.setField(saved, "id", commentId);
                                ReflectionTestUtils.setField(saved, "createdAt", createdAt);
                                return saved;
                            });

            CommentResult result = commentApplication.register(authorId, articleId, "첫 댓글");

            ArgumentCaptor<Comment> saved = ArgumentCaptor.forClass(Comment.class);
            verify(commentRepository).save(saved.capture());
            assertThat(saved.getValue().getUser()).isSameAs(author);
            assertThat(saved.getValue().getArticle()).isSameAs(article);
            assertThat(result)
                    .isEqualTo(
                            new CommentResult(
                                    commentId, articleId, authorId, "작성자", "첫 댓글", 0L, false,
                                    createdAt));
        }

        @Test
        @DisplayName("댓글을 등록하면 기사의 댓글 수를 1 늘린다")
        void increaseArticleCommentCount() {
            User author = author();
            Article article = mock(Article.class);
            given(article.getId()).willReturn(articleId);
            given(userService.getActiveUser(authorId)).willReturn(author);
            given(articleService.findById(articleId)).willReturn(article);
            given(commentRepository.save(any(Comment.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            commentApplication.register(authorId, articleId, "첫 댓글");

            verify(articleService).increaseCommentCount(articleId);
        }

        @Test
        @DisplayName("없거나 탈퇴한 사용자면 USER_NOT_FOUND 예외가 나고 기사 조회·저장을 하지 않는다")
        void notExistUser() {
            given(userService.getActiveUser(authorId))
                    .willThrow(new MonewException(UserErrorType.USER_NOT_FOUND));

            assertThatThrownBy(() -> commentApplication.register(authorId, articleId, "첫 댓글"))
                    .isInstanceOf(MonewException.class)
                    .extracting("errorType")
                    .isEqualTo(UserErrorType.USER_NOT_FOUND);
            verifyNoInteractions(articleService, commentRepository);
        }

        @Test
        @DisplayName("없거나 삭제된 기사면 ARTICLE_NOT_FOUND 예외가 나고 저장·댓글 수 증가를 하지 않는다")
        void notExistArticle() {
            given(userService.getActiveUser(authorId)).willReturn(mock(User.class));
            given(articleService.findById(articleId))
                    .willThrow(new MonewException(ArticleErrorType.ARTICLE_NOT_FOUND));

            assertThatThrownBy(() -> commentApplication.register(authorId, articleId, "첫 댓글"))
                    .isInstanceOf(MonewException.class)
                    .extracting("errorType")
                    .isEqualTo(ArticleErrorType.ARTICLE_NOT_FOUND);
            verifyNoInteractions(commentRepository);
            verify(articleService, never()).increaseCommentCount(any());
        }
    }

    @Nested
    @DisplayName("댓글 논리 삭제")
    class SoftDelete {

        @Test
        @DisplayName("댓글을 논리 삭제하면 DB에서 살아 있을 때만 삭제 시각을 넣고, 바뀐 줄이 1이면 기사의 댓글 수를 1 줄인다")
        void success() {
            Article article = mock(Article.class);
            given(article.getId()).willReturn(articleId);
            Comment comment = savedComment(mock(User.class), article);
            given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
            given(commentRepository.softDeleteIfActive(eq(commentId), any())).willReturn(1);

            commentApplication.softDeleteComment(commentId);

            verify(commentRepository).softDeleteIfActive(eq(commentId), any());
            verify(articleService).decreaseCommentCount(articleId);
        }

        @Test
        @DisplayName("읽은 뒤 다른 요청이 먼저 논리 삭제해서 바뀐 줄이 0이면 COMMENT_NOT_FOUND 예외가 나고 기사의 댓글 수를 줄이지 않는다")
        void deletedByOtherRequest() {
            Comment comment = savedComment(mock(User.class), mock(Article.class));
            given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
            given(commentRepository.softDeleteIfActive(eq(commentId), any())).willReturn(0);

            assertThatThrownBy(() -> commentApplication.softDeleteComment(commentId))
                    .isInstanceOf(MonewException.class)
                    .extracting("errorType")
                    .isEqualTo(CommentErrorType.COMMENT_NOT_FOUND);
            verify(articleService, never()).decreaseCommentCount(any());
        }

        @Test
        @DisplayName("이미 삭제된 댓글이면 COMMENT_NOT_FOUND 예외가 나고 기사의 댓글 수를 줄이지 않는다")
        void deletedComment() {
            Comment comment = savedComment(mock(User.class), mock(Article.class));
            comment.softDelete();
            given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));

            assertThatThrownBy(() -> commentApplication.softDeleteComment(commentId))
                    .isInstanceOf(MonewException.class)
                    .extracting("errorType")
                    .isEqualTo(CommentErrorType.COMMENT_NOT_FOUND);
            verify(commentRepository, never()).softDeleteIfActive(any(), any());
            verify(articleService, never()).decreaseCommentCount(any());
        }

        @Test
        @DisplayName("없는 댓글이면 COMMENT_NOT_FOUND 예외가 나고 기사의 댓글 수를 줄이지 않는다")
        void notExistComment() {
            given(commentRepository.findById(commentId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> commentApplication.softDeleteComment(commentId))
                    .isInstanceOf(MonewException.class)
                    .extracting("errorType")
                    .isEqualTo(CommentErrorType.COMMENT_NOT_FOUND);
            verifyNoInteractions(articleService);
        }
    }

    @Nested
    @DisplayName("댓글 물리 삭제")
    class HardDelete {

        @Test
        @DisplayName("살아 있는 댓글을 물리 삭제하면 좋아요를 먼저 지운 뒤 댓글을 지우고, 기사의 댓글 수를 1 줄인다")
        void activeComment() {
            Article article = mock(Article.class);
            given(article.getId()).willReturn(articleId);
            Comment comment = savedComment(mock(User.class), article);
            given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
            given(commentRepository.softDeleteIfActive(eq(commentId), any())).willReturn(1);
            given(commentRepository.hardDeleteById(commentId)).willReturn(1);

            commentApplication.hardDeleteComment(commentId);

            // 살아 있는지는 DB에서 조건부로 바꿔 보고 판단한다
            // 좋아요가 댓글을 가리키고 있어서 좋아요를 먼저 지워야 댓글을 지울 수 있다
            InOrder inOrder = inOrder(commentLikeRepository, commentRepository);
            inOrder.verify(commentRepository).softDeleteIfActive(eq(commentId), any());
            inOrder.verify(commentLikeRepository).deleteAllByCommentId(commentId);
            inOrder.verify(commentRepository).hardDeleteById(commentId);
            verify(articleService).decreaseCommentCount(articleId);
        }

        @Test
        @DisplayName("댓글을 물리 삭제하면 그 댓글을 가리키는 좋아요 알림도 지운다")
        void deleteCommentNotifications() {
            Comment comment = savedComment(mock(User.class), mock(Article.class));
            given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
            given(commentRepository.softDeleteIfActive(eq(commentId), any())).willReturn(1);
            given(commentRepository.hardDeleteById(commentId)).willReturn(1);

            commentApplication.hardDeleteComment(commentId);

            // 물리 삭제할 때는 그 댓글의 좋아요 알림도 같이 지워야 한다
            verify(notificationService).deleteByResource(ResourceType.COMMENT, commentId);
        }

        @Test
        @DisplayName("이미 논리 삭제된 댓글이면 좋아요와 댓글은 지우지만, 기사의 댓글 수는 다시 줄이지 않는다")
        void softDeletedComment() {
            Comment comment = savedComment(mock(User.class), mock(Article.class));
            comment.softDelete();
            given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
            given(commentRepository.softDeleteIfActive(eq(commentId), any())).willReturn(0);
            given(commentRepository.hardDeleteById(commentId)).willReturn(1);

            commentApplication.hardDeleteComment(commentId);

            // 논리 삭제할 때 이미 1 줄였으므로 또 줄이면 댓글 수가 실제보다 적어진다
            verify(commentLikeRepository).deleteAllByCommentId(commentId);
            verify(commentRepository).hardDeleteById(commentId);
            verify(articleService, never()).decreaseCommentCount(any());
        }

        @Test
        @DisplayName("읽을 땐 살아 있었어도 그 사이 다른 요청이 논리 삭제해서 바뀐 줄이 0이면, 지우기만 하고 기사의 댓글 수는 줄이지 않는다")
        void softDeletedByOtherRequest() {
            Comment comment = savedComment(mock(User.class), mock(Article.class));
            given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
            given(commentRepository.softDeleteIfActive(eq(commentId), any())).willReturn(0);
            given(commentRepository.hardDeleteById(commentId)).willReturn(1);

            commentApplication.hardDeleteComment(commentId);

            // 다른 요청의 논리 삭제가 이미 1 줄였다
            verify(commentLikeRepository).deleteAllByCommentId(commentId);
            verify(commentRepository).hardDeleteById(commentId);
            verify(articleService, never()).decreaseCommentCount(any());
        }

        @Test
        @DisplayName("읽은 뒤 다른 요청이 먼저 물리 삭제해서 지운 줄이 0이면 COMMENT_NOT_FOUND 예외가 나고 댓글 수와 알림은 건드리지 않는다")
        void hardDeletedByOtherRequest() {
            Comment comment = savedComment(mock(User.class), mock(Article.class));
            comment.softDelete();
            given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
            given(commentRepository.softDeleteIfActive(eq(commentId), any())).willReturn(0);
            given(commentRepository.hardDeleteById(commentId)).willReturn(0);

            assertThatThrownBy(() -> commentApplication.hardDeleteComment(commentId))
                    .isInstanceOf(MonewException.class)
                    .extracting("errorType")
                    .isEqualTo(CommentErrorType.COMMENT_NOT_FOUND);

            // 먼저 지운 요청이 댓글 수와 알림을 이미 정리했다
            verify(articleService, never()).decreaseCommentCount(any());
            verifyNoInteractions(notificationService);
        }

        @Test
        @DisplayName("없는 댓글이면 COMMENT_NOT_FOUND 예외가 나고 아무것도 지우지 않는다")
        void notExistComment() {
            given(commentRepository.findById(commentId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> commentApplication.hardDeleteComment(commentId))
                    .isInstanceOf(MonewException.class)
                    .extracting("errorType")
                    .isEqualTo(CommentErrorType.COMMENT_NOT_FOUND);
            verify(commentRepository, never()).softDeleteIfActive(any(), any());
            verify(commentRepository, never()).hardDeleteById(any());
            verifyNoInteractions(commentLikeRepository, articleService, notificationService);
        }
    }

    @Nested
    @DisplayName("기사 물리 삭제 시 좋아요 알림 삭제")
    class DeleteLikeNotificationsByArticle {

        @Test
        @DisplayName("기사의 댓글마다 그 댓글을 가리키는 좋아요 알림을 지운다")
        void deleteEachCommentNotifications() {
            UUID firstCommentId = UUID.randomUUID();
            UUID secondCommentId = UUID.randomUUID();
            given(commentRepository.findIdsByArticleId(articleId))
                    .willReturn(List.of(firstCommentId, secondCommentId));

            commentApplication.deleteLikeNotificationsByArticle(articleId);

            verify(notificationService).deleteByResource(ResourceType.COMMENT, firstCommentId);
            verify(notificationService).deleteByResource(ResourceType.COMMENT, secondCommentId);
        }

        @Test
        @DisplayName("기사에 댓글이 없으면 알림을 지우지 않는다")
        void noComments() {
            given(commentRepository.findIdsByArticleId(articleId)).willReturn(List.of());

            commentApplication.deleteLikeNotificationsByArticle(articleId);

            verifyNoInteractions(notificationService);
        }
    }

    @Nested
    @DisplayName("댓글 목록 조회")
    class FindByQuery {

        private final UUID requesterId = UUID.randomUUID();
        private final CommentQuery query =
                new CommentQuery(
                        articleId,
                        CommentQuery.SortOrder.CREATED_AT,
                        SortDirection.DESC,
                        null,
                        null,
                        2);

        private User author;
        private Article article;

        @BeforeEach
        void setUp() {
            RequestIdContext.set(requesterId);
            author = mock(User.class);
            article = mock(Article.class);
        }

        // 페이지에 담기는 댓글만 결과로 바뀌어서, 작성자·기사 정보는 그때만 읽게 따로 준비함
        private void givenConvertible() {
            given(author.getId()).willReturn(authorId);
            given(author.getNickname()).willReturn("작성자");
            given(article.getId()).willReturn(articleId);
        }

        private Comment comment(Instant createdAt) {
            Comment comment = Comment.create(author, article, "댓글");
            ReflectionTestUtils.setField(comment, "id", UUID.randomUUID());
            ReflectionTestUtils.setField(comment, "createdAt", createdAt);
            return comment;
        }

        @Test
        @DisplayName("limit보다 하나 더 가져오면 limit개만 담고, 마지막 댓글로 다음 커서를 만든다")
        void hasNext() {
            givenConvertible();
            Comment first = comment(Instant.parse("2026-11-03T03:00:00Z"));
            Comment second = comment(Instant.parse("2026-11-03T02:00:00Z"));
            Comment extra = comment(Instant.parse("2026-11-03T01:00:00Z"));
            given(commentRepository.findByQuery(query, null))
                    .willReturn(List.of(first, second, extra));
            given(commentRepository.countByQuery(query)).willReturn(5L);

            PageResult<CommentResult> result = commentApplication.findByQuery(query);

            assertThat(result.content())
                    .extracting(CommentResult::id)
                    .containsExactly(first.getId(), second.getId());
            assertThat(result.hasNext()).isTrue();
            // 다음 커서는 정렬 기준과 상관없이 마지막 댓글의 id
            assertThat(result.nextCursor()).isEqualTo(second.getId().toString());
            assertThat(result.nextAfter()).isEqualTo(Instant.parse("2026-11-03T02:00:00Z"));
            assertThat(result.size()).isEqualTo(2);
            assertThat(result.totalElements()).isEqualTo(5L);
        }

        @Test
        @DisplayName("마지막 페이지면 다음 페이지가 없고 커서도 비어 있다")
        void lastPage() {
            givenConvertible();
            Comment only = comment(Instant.parse("2026-11-03T03:00:00Z"));
            given(commentRepository.findByQuery(query, null)).willReturn(List.of(only));
            given(commentRepository.countByQuery(query)).willReturn(1L);

            PageResult<CommentResult> result = commentApplication.findByQuery(query);

            assertThat(result.content())
                    .extracting(CommentResult::id)
                    .containsExactly(only.getId());
            assertThat(result.hasNext()).isFalse();
            assertThat(result.nextCursor()).isNull();
            assertThat(result.nextAfter()).isNull();
        }

        @Test
        @DisplayName("요청자가 좋아요 누른 댓글만 likedByMe가 true이고, 좋아요 여부는 페이지에 담긴 댓글로만 묻는다")
        void likedByMe() {
            givenConvertible();
            Comment liked = comment(Instant.parse("2026-11-03T03:00:00Z"));
            Comment notLiked = comment(Instant.parse("2026-11-03T02:00:00Z"));
            Comment extra = comment(Instant.parse("2026-11-03T01:00:00Z"));
            given(commentRepository.findByQuery(query, null))
                    .willReturn(List.of(liked, notLiked, extra));
            given(commentRepository.countByQuery(query)).willReturn(3L);
            given(commentLikeRepository.findLikedCommentIds(eq(requesterId), any()))
                    .willReturn(Set.of(liked.getId()));

            PageResult<CommentResult> result = commentApplication.findByQuery(query);

            assertThat(result.content())
                    .extracting(CommentResult::likedByMe)
                    .containsExactly(true, false);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Collection<UUID>> idsCaptor = ArgumentCaptor.forClass(Collection.class);
            verify(commentLikeRepository).findLikedCommentIds(eq(requesterId), idsCaptor.capture());
            assertThat(idsCaptor.getValue())
                    .containsExactlyInAnyOrder(liked.getId(), notLiked.getId());
        }

        @Test
        @DisplayName("댓글이 없으면 빈 페이지를 돌려준다")
        void empty() {
            given(commentRepository.findByQuery(query, null)).willReturn(List.of());
            given(commentRepository.countByQuery(query)).willReturn(0L);

            PageResult<CommentResult> result = commentApplication.findByQuery(query);

            assertThat(result.content()).isEmpty();
            assertThat(result.hasNext()).isFalse();
            assertThat(result.totalElements()).isZero();
        }

        private CommentQuery nextPageQuery(String cursor, Instant after) {
            return new CommentQuery(
                    articleId,
                    CommentQuery.SortOrder.CREATED_AT,
                    SortDirection.DESC,
                    cursor,
                    after,
                    2);
        }

        @Test
        @DisplayName("다음 페이지면 커서 id의 댓글을 찾아 저장소에 넘기고, 논리 삭제된 댓글도 커서로 쓴다")
        void withCursorComment() {
            Comment cursorComment = comment(Instant.parse("2026-11-03T03:00:00Z"));
            ReflectionTestUtils.setField(cursorComment, "deletedAt", Instant.now());
            CommentQuery next =
                    nextPageQuery(cursorComment.getId().toString(), cursorComment.getCreatedAt());
            given(commentRepository.findById(cursorComment.getId()))
                    .willReturn(Optional.of(cursorComment));
            given(commentRepository.findByQuery(next, cursorComment)).willReturn(List.of());
            given(commentRepository.countByQuery(next)).willReturn(0L);

            commentApplication.findByQuery(next);

            verify(commentRepository).findByQuery(next, cursorComment);
        }

        @Test
        @DisplayName("커서 댓글이 물리 삭제돼 없으면 COMMENT_NOT_FOUND 예외가 나고 목록을 조회하지 않는다")
        void cursorCommentNotFound() {
            UUID missingId = UUID.randomUUID();
            CommentQuery next = nextPageQuery(missingId.toString(), Instant.now());
            given(commentRepository.findById(missingId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> commentApplication.findByQuery(next))
                    .isInstanceOf(MonewException.class)
                    .extracting("errorType")
                    .isEqualTo(CommentErrorType.COMMENT_NOT_FOUND);
            verify(commentRepository, never()).findByQuery(any(), any());
        }

        @Test
        @DisplayName("커서가 UUID 모양이 아니면 INVALID_REQUEST 예외가 나고 DB를 조회하지 않는다")
        void invalidCursor() {
            CommentQuery next = nextPageQuery("abc", Instant.now());

            assertThatThrownBy(() -> commentApplication.findByQuery(next))
                    .isInstanceOf(MonewException.class)
                    .extracting("errorType")
                    .isEqualTo(CommonErrorType.INVALID_REQUEST);
            verifyNoInteractions(commentRepository);
        }

        @Test
        @DisplayName("커서와 after 중 하나만 주면 예외가 나고 DB를 조회하지 않는다")
        void cursorWithoutAfter() {
            CommentQuery next = nextPageQuery(UUID.randomUUID().toString(), null);

            assertThatThrownBy(() -> commentApplication.findByQuery(next))
                    .isInstanceOf(MonewException.class)
                    .extracting("errorType")
                    .isEqualTo(CommonErrorType.PAGINATION_NOT_BOTH_CURSOR_AND_AFTER);
            verifyNoInteractions(commentRepository);
        }
    }
}
