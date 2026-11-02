package com.sprint.monew.application.comment;

import com.sprint.monew.application.comment.provided.dto.CommentQuery;
import com.sprint.monew.domain.comment.entity.Comment;
import com.sprint.monew.domain.comment.entity.CommentLike;
import com.sprint.monew.domain.user.entity.User;
import com.sprint.monew.global.pagination.PageResult;
import java.util.Collection;
import java.util.Set;
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

    PageResult<Comment> findPageByQuery(CommentQuery query);

    Set<UUID> findLikedCommentIds(UUID userId, Collection<UUID> commentIds);
}
