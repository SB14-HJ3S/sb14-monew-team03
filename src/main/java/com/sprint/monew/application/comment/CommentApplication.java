package com.sprint.monew.application.comment;

import com.sprint.monew.application.article.IArticleService;
import com.sprint.monew.application.comment.provided.CommentUseCase;
import com.sprint.monew.application.comment.provided.dto.CommentLikeResult;
import com.sprint.monew.application.comment.provided.dto.CommentQuery;
import com.sprint.monew.application.comment.provided.dto.CommentResult;
import com.sprint.monew.application.comment.required.CommentEventPublisherPort;
import com.sprint.monew.application.user.IUserService;
import com.sprint.monew.domain.article.entity.Article;
import com.sprint.monew.domain.comment.entity.Comment;
import com.sprint.monew.domain.comment.entity.CommentLike;
import com.sprint.monew.domain.comment.event.CommentLikedEvent;
import com.sprint.monew.domain.user.entity.User;
import com.sprint.monew.global.annotation.ApplicationService;
import com.sprint.monew.global.context.RequestIdContext;
import com.sprint.monew.global.pagination.PageResult;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@ApplicationService
@RequiredArgsConstructor
public class CommentApplication implements CommentUseCase {

    private final ICommentService commentService;
    private final IUserService userService;
    private final IArticleService articleService;
    private final CommentEventPublisherPort commentEventPublisher;

    @Override
    @Transactional
    public CommentResult register(UUID userId, UUID articleId, String content) {
        User user = userService.getActiveUser(userId);
        Article article = articleService.findById(articleId);

        Comment comment = commentService.saveComment(Comment.create(user, article, content));

        // 방금 만든 댓글이라 아직 아무도 좋아요를 누르지 않았다
        // +1 쿼리 뒤에는 댓글 정보를 못 읽을 수 있어서 결과를 먼저 만든다
        CommentResult result = CommentResult.of(comment, false);

        articleService.increaseCommentCount(articleId);

        return result;
    }

    @Override
    public PageResult<Comment> findByQuery(CommentQuery query) {
        return null;
    }

    @Override
    public Comment findById(UUID commentId) {
        return null;
    }

    @Override
    @Transactional
    public CommentLikeResult like(UUID commentId) {
        UUID requestUserId = RequestIdContext.get();
        User user = userService.getActiveUser(requestUserId);
        Comment comment = commentService.getActiveComment(commentId);

        CommentLike commentLike = commentService.saveLike(user, comment);

        // +1 쿼리 뒤에는 댓글 정보를 못 읽을 수 있어서 알림 이벤트를 먼저 보낸다
        commentEventPublisher.publish(
                new CommentLikedEvent(
                        comment.getId(),
                        comment.getUser().getId(),
                        user.getId(),
                        user.getNickname()));

        commentService.increaseLikeCount(commentId);

        // 늘어난 좋아요 수를 응답에 넣으려고 댓글을 다시 읽는다
        return CommentLikeResult.of(commentLike, commentService.getComment(commentId));
    }

    @Override
    @Transactional
    public void unlike(UUID commentId) {
        UUID requestUserId = RequestIdContext.get();
        commentService.getActiveComment(commentId);

        commentService.deleteLike(commentId, requestUserId);
        commentService.decreaseLikeCount(commentId);
    }

    @Override
    @Transactional
    public CommentResult changeContent(UUID commentId, String content) {
        UUID requestUserId = RequestIdContext.get();
        Comment comment = commentService.getComment(commentId);

        comment.changeContent(requestUserId, content);

        boolean likedByMe = commentService.isLikedBy(commentId, requestUserId);

        return CommentResult.of(comment, likedByMe);
    }

    @Override
    @Transactional
    public void softDeleteComment(UUID commentId) {
        Comment comment = commentService.getActiveComment(commentId);

        comment.softDelete();

        articleService.decreaseCommentCount(comment.getArticle().getId());
    }

    @Override
    @Transactional
    public void hardDeleteComment(UUID commentId) {
        Comment comment = commentService.getComment(commentId);

        // 삭제 쿼리 뒤에는 댓글 정보를 못 읽을 수 있어서 먼저 꺼내 둔다
        boolean wasActive = !comment.isDeleted();
        UUID articleId = comment.getArticle().getId();

        // 좋아요가 댓글을 가리키고 있어서 좋아요부터 지운다
        commentService.deleteAllLikes(commentId);
        commentService.deleteComment(commentId);

        // 이미 논리 삭제된 댓글은 그때 1 줄였으므로 다시 줄이지 않는다
        if (wasActive) {
            articleService.decreaseCommentCount(articleId);
        }
    }
}
