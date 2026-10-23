package com.sprint.monew.application.notification;

import com.sprint.monew.application.notification.provided.NotificationUseCase;
import com.sprint.monew.application.notification.provided.dto.NotificationQuery;
import com.sprint.monew.application.notification.required.NotificationRepositoryPort;
import com.sprint.monew.domain.notification.entity.Notification;
import com.sprint.monew.global.pagination.PageResult;
import com.sprint.monew.global.type.ResourceType;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationApplication implements NotificationUseCase {

    private final NotificationRepositoryPort notificationRepository;

    @Override
    public PageResult<Notification> findByQuery(NotificationQuery query) {
        return null;
    }

    @Override
    public void markAllAsRead(UUID requestId) {}

    @Override
    public void markAsRead(UUID notificationId, UUID requestId) {}

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createArticleNotifications(UUID interestId, String interestName, int articleCount) {

        String content = String.format("%s와 관련된 기사가 %d건 등록되었습니다.", interestName, articleCount);

        List<Notification> notifications =
                subscriberReader.findSubscriberIds(interestId).stream()
                        .map(notificationRepository::getUserReference)
                        .map(
                                user ->
                                        Notification.create(
                                                user, ResourceType.INTEREST, interestId, content))
                        .toList();

        if (notifications.isEmpty()) {
            return;
        }
        notificationRepository.saveAll(notifications);
        log.info("기사 알림 생성: interestId={}, 대상={}명", interestId, notifications.size());
    }

    @Override
    public void createCommentLikeNotification(
            UUID commentId, UUID authorId, UUID likerId, String likerNickname) {}
}
