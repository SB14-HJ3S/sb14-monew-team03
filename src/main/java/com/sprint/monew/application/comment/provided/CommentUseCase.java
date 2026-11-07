package com.sprint.monew.application.comment.provided;

import com.sprint.monew.application.comment.provided.dto.CommentLikeResult;
import com.sprint.monew.application.comment.provided.dto.CommentQuery;
import com.sprint.monew.application.comment.provided.dto.CommentResult;
import com.sprint.monew.domain.comment.entity.Comment;
import com.sprint.monew.global.pagination.PageResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public interface CommentUseCase {
    CommentResult register(
            @NotNull(message = "사용자 ID는 필수입니다.") UUID userId,
            @NotNull(message = "기사 ID는 필수입니다.") UUID articleId,
            @NotBlank(message = "댓글 내용은 필수입니다.") String content);

    PageResult<CommentResult> findByQuery(
            @NotNull(message = "조회 조건은 필수입니다.") @Valid CommentQuery query);

    Comment findById(@NotNull(message = "댓글 ID는 필수입니다.") UUID commentId);

    CommentLikeResult like(@NotNull(message = "댓글 ID는 필수입니다.") UUID commentId);

    void unlike(@NotNull(message = "댓글 ID는 필수입니다.") UUID commentId);

    CommentResult changeContent(
            @NotNull(message = "댓글 ID는 필수입니다.") UUID commentId,
            @NotBlank(message = "댓글 내용은 필수입니다.") String content);

    void softDeleteComment(@NotNull(message = "댓글 ID는 필수입니다.") UUID commentId);

    void hardDeleteComment(@NotNull(message = "댓글 ID는 필수입니다.") UUID commentId);

    void deleteLikeNotificationsByArticle(@NotNull(message = "기사 ID는 필수입니다.") UUID articleId);

    // 사용자 완전 삭제 직전에 같은 트랜잭션에서 호출함
    void cleanupCountsBeforeUserHardDeletion(
            @NotNull(message = "사용자 ID 목록은 필수입니다.") List<UUID> userIds);
}
