package com.sprint.monew.application.notification;

import com.sprint.monew.application.notification.provided.dto.NotificationQuery;
import com.sprint.monew.application.notification.required.NotificationRepositoryPort;
import com.sprint.monew.domain.notification.entity.Notification;
import com.sprint.monew.domain.user.entity.User;
import com.sprint.monew.global.pagination.PageResult;
import com.sprint.monew.global.type.ResourceType;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepositoryPort notificationRepository;

    @Override
    public void createArticleNotifications(
            List<User> users, UUID interestId, String interestName, int articleCount) {
        String content = String.format("%s와 관련된 기사가 %d건 등록되었습니다.", interestName, articleCount);

        List<Notification> notifications =
                users.stream()
                        .map(
                                user ->
                                        Notification.create(
                                                user, ResourceType.INTEREST, interestId, content))
                        .toList();

        if (notifications.isEmpty()) {
            return;
        }
        notificationRepository.saveAll(notifications);
    }

    @Override
    public void createCommentLikeNotification(
            User commentUser, UUID commentId, String likedByNickname) {
        String content = String.format("%s님이 나의 댓글을 좋아합니다.", likedByNickname);

        notificationRepository.save(
                Notification.create(commentUser, ResourceType.COMMENT, commentId, content));
    }

    @Override
    public PageResult<Notification> findUnread(UUID userId, NotificationQuery query) {
        List<Notification> fetched = notificationRepository.findByQuery(userId, query);
        long total = notificationRepository.countByQuery(userId, query);

        return PageResult.of(
                fetched, total, query, n -> n.getId().toString(), Notification::getCreatedAt);
    }
}
