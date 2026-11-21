package com.sprint.monew.application.comment.provided.service;

import com.sprint.monew.application.comment.provided.usecase.dto.CommentQuery;
import com.sprint.monew.domain.comment.entity.Comment;
import com.sprint.monew.domain.comment.entity.CommentLike;
import com.sprint.monew.domain.user.entity.User;
import com.sprint.monew.global.pagination.PageResult;
import java.util.Collection;
import java.util.List;
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

    // 논리 삭제된 댓글도 포함한다
    List<UUID> findIdsByArticleId(UUID articleId);

    // 사용자 완전 삭제 전에, 탈퇴자가 누른 좋아요만큼 댓글마다 좋아요 수를 줄임
    void decreaseLikeCountsByUserIds(List<UUID> userIds);
}
