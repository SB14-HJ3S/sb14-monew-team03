package com.sprint.monew.application.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sprint.monew.application.notification.provided.usecase.dto.NotificationQuery;
import com.sprint.monew.application.notification.required.NotificationRepositoryPort;
import com.sprint.monew.domain.notification.entity.Notification;
import com.sprint.monew.domain.user.entity.User;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.NotificationErrorType;
import com.sprint.monew.global.pagination.PageResult;
import com.sprint.monew.global.type.ResourceType;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

    @Test
    @DisplayName("기사 알림은 구독자마다 하나씩 만들어 한 번에 저장한다.")
    void givenSubscribers_whenCreateArticleNotifications_thenSavesOnePerUser() {
        User woody = User.create("woody@monew.com", "우디", "pw");
        User buzz = User.create("buzz@monew.com", "버즈", "pw");
        UUID interestId = UUID.randomUUID();

        notificationService.createArticleNotifications(List.of(woody, buzz), interestId, "축구", 3);

        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(captor.capture());
        List<Notification> saved = captor.getValue();
        assertThat(saved).hasSize(2);
        assertThat(saved).extracting(Notification::getUser).containsExactly(woody, buzz);
        assertThat(saved)
                .allSatisfy(
                        n -> {
                            assertThat(n.getContent()).isEqualTo("축구와 관련된 기사가 3건 등록되었습니다.");
                            assertThat(n.getResourceType()).isEqualTo(ResourceType.INTEREST);
                            assertThat(n.getResourceId()).isEqualTo(interestId);
                        });
    }

    @Test
    @DisplayName("구독자가 없으면 기사 알림을 저장하지 않는다.")
    void givenNoSubscribers_whenCreateArticleNotifications_thenSavesNothing() {
        notificationService.createArticleNotifications(List.of(), UUID.randomUUID(), "축구", 3);

        verify(notificationRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("알림 목록은 조회 결과와 전체 개수로 페이지를 만들고, 다음 페이지가 있으면 마지막 알림으로 커서를 만든다.")
    void givenMoreThanLimit_whenFindUnread_thenNextCursorFromLastItem() {
        UUID userId = UUID.randomUUID();
        NotificationQuery query = NotificationQuery.of(null, null, 1); // limit 1
        UUID firstId = UUID.randomUUID();
        Instant firstCreatedAt = Instant.parse("2026-11-06T01:00:00Z");
        Notification first = mock(Notification.class);
        Notification second = mock(Notification.class);
        when(first.getId()).thenReturn(firstId);
        when(first.getCreatedAt()).thenReturn(firstCreatedAt);
        // limit보다 하나 더 조회돼서 다음 페이지가 있다는 걸 알 수 있다
        when(notificationRepository.findByQuery(userId, query)).thenReturn(List.of(first, second));
        when(notificationRepository.countByQuery(userId, query)).thenReturn(5L);

        PageResult<Notification> result = notificationService.findUnread(userId, query);

        assertThat(result.content()).containsExactly(first);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.nextCursor()).isEqualTo(firstId.toString());
        assertThat(result.nextAfter()).isEqualTo(firstCreatedAt);
        assertThat(result.totalElements()).isEqualTo(5L);
    }

    @Test
    @DisplayName("확인한 지 기준 시각이 지난 알림 삭제는 기준 시각을 그대로 넘기고 지운 개수를 돌려준다.")
    void whenDeleteReadBefore_thenDelegatesAndReturnsCount() {
        LocalDateTime threshold = LocalDateTime.of(2026, 10, 30, 0, 0);
        when(notificationRepository.deleteReadBefore(threshold)).thenReturn(4);

        assertThat(notificationService.deleteReadBefore(threshold)).isEqualTo(4);
    }
}
