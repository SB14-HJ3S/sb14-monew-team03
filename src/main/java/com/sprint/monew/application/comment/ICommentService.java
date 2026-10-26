package com.sprint.monew.application.comment;

import com.sprint.monew.domain.comment.entity.Comment;
import com.sprint.monew.domain.comment.entity.CommentLike;
import com.sprint.monew.domain.user.entity.User;
import java.util.UUID;

public interface ICommentService {
    Comment getComment(UUID commentId);

    Comment getActiveComment(UUID commentId);

    Comment saveComment(Comment comment);

    boolean isLikedBy(UUID commentId, UUID userId);

    CommentLike saveLike(User user, Comment comment);

    void deleteLike(UUID commentId, UUID userId);

    void increaseLikeCount(UUID commentId);

    void decreaseLikeCount(UUID commentId);
}
