package com.sprint.monew.application.notification;

import com.sprint.monew.application.notification.provided.dto.NotificationQuery;
import com.sprint.monew.application.notification.required.NotificationRepositoryPort;
import com.sprint.monew.domain.notification.entity.Notification;
import com.sprint.monew.domain.user.entity.User;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.NotificationErrorType;
import com.sprint.monew.global.pagination.PageResult;
import com.sprint.monew.global.type.ResourceType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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

    @Override
    public int markAllAsRead(UUID userId) {
        return notificationRepository.markAllAsRead(userId, LocalDateTime.now());
    }

    @Override
    public void markAsRead(UUID userId, UUID notificationId) {
        Notification notification =
                notificationRepository
                        .findById(notificationId)
                        // 남의 알림도 존재 여부를 노출하지 않도록 없는 것과 같이 처리한다
                        .filter(n -> n.getUser().getId().equals(userId))
                        .orElseThrow(
                                () ->
                                        new MonewException(
                                                NotificationErrorType.NOTIFICATION_NOT_FOUND,
                                                Map.of("notificationId", notificationId)));

        notification.markAsRead(LocalDateTime.now());
    }

    @Override
    public int deleteReadBefore(LocalDateTime threshold) {
        return notificationRepository.deleteReadBefore(threshold);
    }

    @Override
    public int deleteByResource(ResourceType resourceType, UUID resourceId) {
        return notificationRepository.deleteByResource(resourceType, resourceId);
    }
}
