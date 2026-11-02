package com.sprint.monew.application.comment;

import com.sprint.monew.domain.comment.entity.Comment;
import com.sprint.monew.domain.comment.entity.CommentLike;
import com.sprint.monew.domain.user.entity.User;
import java.util.List;
import java.util.UUID;

public interface CommentService {
    Comment getComment(UUID commentId);

    Comment getActiveComment(UUID commentId);

    Comment saveComment(Comment comment);

    boolean isLikedBy(UUID commentId, UUID userId);

    CommentLike saveLike(User user, Comment comment);

    void deleteLike(UUID commentId, UUID userId);

    void increaseLikeCount(UUID commentId);

    void decreaseLikeCount(UUID commentId);

    void softDelete(UUID commentId);

    // 지우기 전에 살아 있던 댓글이었으면 true
    boolean hardDelete(UUID commentId);

    // 논리 삭제된 댓글도 포함한다
    List<UUID> findIdsByArticleId(UUID articleId);
}
