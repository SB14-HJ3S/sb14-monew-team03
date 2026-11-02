package com.sprint.monew.application.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import com.sprint.monew.application.notification.required.NotificationRepositoryPort;
import com.sprint.monew.domain.notification.entity.Notification;
import com.sprint.monew.domain.user.entity.User;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.NotificationErrorType;
import com.sprint.monew.global.type.ResourceType;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock NotificationRepositoryPort notificationRepository;
    @InjectMocks NotificationServiceImpl notificationService;

    @Test
    void 내_알림을_확인하면_읽음으로_바뀐다() {
        UUID userId = UUID.randomUUID();
        Notification notification = notificationOf(userId);
        given(notificationRepository.findById(notification.getId()))
                .willReturn(Optional.of(notification));

        notificationService.markAsRead(userId, notification.getId());

        assertThat(notification.isRead()).isTrue();
        assertThat(notification.getReadAt()).isNotNull();
    }

    @Test
    void 없는_알림을_확인하면_NOT_FOUND() {
        UUID notificationId = UUID.randomUUID();
        given(notificationRepository.findById(notificationId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(UUID.randomUUID(), notificationId))
                .isInstanceOf(MonewException.class)
                .extracting("errorType") // MonewException 필드 이름으로
                .isEqualTo(NotificationErrorType.NOTIFICATION_NOT_FOUND);
    }

    @Test
    void 남의_알림을_확인하면_NOT_FOUND이고_상태는_그대로() {
        UUID ownerId = UUID.randomUUID();
        Notification notification = notificationOf(ownerId);
        given(notificationRepository.findById(notification.getId()))
                .willReturn(Optional.of(notification));

        assertThatThrownBy(
                        () ->
                                notificationService.markAsRead(
                                        UUID.randomUUID(), notification.getId()))
                .isInstanceOf(MonewException.class)
                .extracting("errorType")
                .isEqualTo(NotificationErrorType.NOTIFICATION_NOT_FOUND);
        assertThat(notification.isRead()).isFalse();
    }

    @Test
    void 전체_확인은_리포지토리에_사용자_ID를_넘긴다() {
        UUID userId = UUID.randomUUID();
        given(notificationRepository.markAllAsRead(eq(userId), any(LocalDateTime.class)))
                .willReturn(3);

        int updated = notificationService.markAllAsRead(userId);

        assertThat(updated).isEqualTo(3);
    }

    @Test
    void 리소스별_삭제는_리포지토리에_종류와_ID를_넘기고_지운_개수를_돌려준다() {
        UUID commentId = UUID.randomUUID();
        given(notificationRepository.deleteByResource(ResourceType.COMMENT, commentId))
                .willReturn(2);

        int deleted = notificationService.deleteByResource(ResourceType.COMMENT, commentId);

        assertThat(deleted).isEqualTo(2);
    }

    private Notification notificationOf(UUID userId) {
        User user = mock(User.class);
        lenient().when(user.getId()).thenReturn(userId);
        Notification notification =
                Notification.create(user, ResourceType.COMMENT, UUID.randomUUID(), "테스트");
        ReflectionTestUtils.setField(notification, "id", UUID.randomUUID());
        return notification;
    }
}
