package com.sprint.monew.application.notification;

import com.sprint.monew.domain.user.entity.User;
import java.util.List;
import java.util.UUID;

public interface NotificationService {
    void createArticleNotifications(
            List<User> users, UUID interestId, String interestName, int articleCount);

    void createCommentLikeNotification(User commentUser, UUID commentId, String likedByNickname);
}
