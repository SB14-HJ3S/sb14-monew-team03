package com.sprint.monew.application.notification.provided.usecase;

import com.sprint.monew.application.notification.provided.usecase.dto.NotificationQuery;
import com.sprint.monew.application.notification.provided.usecase.dto.NotificationResult;
import com.sprint.monew.global.pagination.PageResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public interface NotificationUseCase {
    PageResult<NotificationResult> findByQuery(@NotNull @Valid NotificationQuery query);

    void markAllAsRead();

    void markAsRead(@NotNull UUID notificationId);

    void createArticleNotifications(
            @NotNull UUID interestId, @NotBlank String interestName, @Min(1) int articleCount);

    void createCommentLikeNotification(
            @NotNull UUID commentId,
            @NotNull UUID commentUserId,
            @NotNull UUID likedById,
            @NotBlank String likedByNickname);

    int deleteOldReadNotifications();
}
