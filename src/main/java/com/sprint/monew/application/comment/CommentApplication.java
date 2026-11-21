package com.sprint.monew.application.comment;

import com.sprint.monew.application.article.provided.service.ArticleService;
import com.sprint.monew.application.comment.event.CommentCreatedEvent;
import com.sprint.monew.application.comment.event.CommentDeletedEvent;
import com.sprint.monew.application.comment.event.CommentLikeCreatedEvent;
import com.sprint.monew.application.comment.event.CommentUnlikedEvent;
import com.sprint.monew.application.comment.event.CommentUpdatedEvent;
import com.sprint.monew.application.comment.provided.service.CommentService;
import com.sprint.monew.application.comment.provided.usecase.CommentUseCase;
import com.sprint.monew.application.comment.provided.usecase.dto.CommentLikeResult;
import com.sprint.monew.application.comment.provided.usecase.dto.CommentQuery;
import com.sprint.monew.application.comment.provided.usecase.dto.CommentResult;
import com.sprint.monew.application.comment.required.CommentEventPublisherPort;
import com.sprint.monew.application.notification.provided.service.NotificationService;
import com.sprint.monew.application.user.provided.service.IUserService;
import com.sprint.monew.domain.article.entity.Article;
import com.sprint.monew.domain.comment.entity.Comment;
import com.sprint.monew.domain.comment.entity.CommentLike;
import com.sprint.monew.domain.comment.event.CommentLikedEvent;
import com.sprint.monew.domain.user.entity.User;
import com.sprint.monew.global.annotation.ApplicationService;
import com.sprint.monew.global.context.RequestIdContext;
import com.sprint.monew.global.pagination.PageResult;
import com.sprint.monew.global.type.ResourceType;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@ApplicationService
@RequiredArgsConstructor
public class CommentApplication implements CommentUseCase {

    private final CommentService commentService;
    private final IUserService userService;
    private final ArticleService articleService;
    private final CommentEventPublisherPort commentEventPublisher;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public CommentResult register(UUID userId, UUID articleId, String content) {
        User user = userService.getActiveUser(userId);
        Article article = articleService.findById(articleId);

        Comment comment = commentService.saveComment(Comment.create(user, article, content));

        // 방금 만든 댓글이라 아직 아무도 좋아요를 누르지 않았다
        // +1 쿼리 뒤에는 댓글 정보를 못 읽을 수 있어서 결과를 먼저 만든다
        CommentResult result = CommentResult.of(comment, false);

        commentEventPublisher.publish(CommentCreatedEvent.of(result, article.getTitle()));

        articleService.increaseCommentCount(articleId);

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<CommentResult> findByQuery(CommentQuery query) {
        UUID requestUserId = RequestIdContext.get();
        PageResult<Comment> page = commentService.findPageByQuery(query);

        // 잘라낸 뒤의 댓글로만 물어봄 (다음 페이지 확인용으로 더 가져온 1개는 뺌)
        List<UUID> commentIds = page.content().stream().map(Comment::getId).toList();
        Set<UUID> likedIds = commentService.findLikedCommentIds(requestUserId, commentIds);

        return page.map(comment -> CommentResult.of(comment, likedIds.contains(comment.getId())));
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
        String articleTitle = comment.getArticle().getTitle();
        commentService.increaseLikeCount(commentId);

        // 늘어난 좋아요 수를 응답에 넣으려고 댓글을 다시 읽는다
        CommentLikeResult result =
                CommentLikeResult.of(commentLike, commentService.getComment(commentId));
        commentEventPublisher.publish(CommentLikeCreatedEvent.of(result, articleTitle));
        return result;
    }

    @Override
    @Transactional
    public void unlike(UUID commentId) {
        UUID requestUserId = RequestIdContext.get();
        commentService.getActiveComment(commentId);

        commentService.deleteLike(commentId, requestUserId);
        commentService.decreaseLikeCount(commentId);

        long likeCount = commentService.getComment(commentId).getLikeCount();
        commentEventPublisher.publish(new CommentUnlikedEvent(commentId, requestUserId, likeCount));
    }

    @Override
    @Transactional
    public CommentResult changeContent(UUID commentId, String content) {
        UUID requestUserId = RequestIdContext.get();
        Comment comment = commentService.getComment(commentId);

        comment.changeContent(requestUserId, content);
        commentEventPublisher.publish(new CommentUpdatedEvent(commentId, comment.getContent()));

        boolean likedByMe = commentService.isLikedBy(commentId, requestUserId);

        return CommentResult.of(comment, likedByMe);
    }

    @Override
    @Transactional
    public void softDeleteComment(UUID commentId) {
        Comment comment = commentService.getActiveComment(commentId);

        // 삭제 쿼리 뒤에는 댓글 정보를 못 읽을 수 있어서 먼저 꺼내 둔다
        UUID articleId = comment.getArticle().getId();

        // 읽은 뒤에 다른 요청이 먼저 지웠으면 여기서 404로 끝나고 댓글 수는 안 줄인다
        commentService.softDelete(commentId);

        articleService.decreaseCommentCount(articleId);
        commentEventPublisher.publish(new CommentDeletedEvent(commentId));
    }

    @Override
    @Transactional
    public void hardDeleteComment(UUID commentId) {
        Comment comment = commentService.getComment(commentId);

        // 삭제 쿼리 뒤에는 댓글 정보를 못 읽을 수 있어서 먼저 꺼내 둔다
        UUID articleId = comment.getArticle().getId();

        // 다른 물리 삭제 요청이 먼저 지웠으면 여기서 404로 끝난다
        // 이미 논리 삭제된 댓글은 그때 1 줄였으므로 다시 줄이지 않는다
        if (commentService.hardDelete(commentId)) {
            articleService.decreaseCommentCount(articleId);
        }

        // 물리 삭제할 때는 이 댓글의 좋아요 알림도 같이 지운다
        notificationService.deleteByResource(ResourceType.COMMENT, commentId);

        commentEventPublisher.publish(new CommentDeletedEvent(commentId));
    }

    @Override
    @Transactional
    public void deleteLikeNotificationsByArticle(UUID articleId) {
        // 기사가 물리 삭제되면 댓글은 DB가 같이 지워서 댓글 물리 삭제 코드를 거치지 않는다
        // 알림은 댓글을 resource_id 값으로만 가리켜서 여기서 직접 지운다
        for (UUID commentId : commentService.findIdsByArticleId(articleId)) {
            notificationService.deleteByResource(ResourceType.COMMENT, commentId);
        }
    }

    @Override
    @Transactional
    public void cleanupCountsBeforeUserHardDeletion(List<UUID> userIds) {
        commentService.decreaseLikeCountsByUserIds(userIds);
        articleService.decreaseCommentCountsByUserIds(userIds);
    }
}
