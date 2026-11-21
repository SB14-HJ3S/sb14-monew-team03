package com.sprint.monew.adapter.out.repository.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sprint.monew.application.notification.provided.usecase.dto.NotificationQuery;
import com.sprint.monew.domain.notification.entity.Notification;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationRepositoryAdapterTest {

    @Mock private NotificationJpaRepository jpaRepository;

    @InjectMocks private NotificationRepositoryAdapter adapter;

    @Test
    @DisplayName("여러 알림을 JPA 리포지토리에 한 번에 저장한다.")
    void saveAll_delegates() {
        List<Notification> notifications = List.of();

        adapter.saveAll(notifications);

        verify(jpaRepository).saveAll(notifications);
    }

    @Test
    @DisplayName("목록 조회는 사용자 ID와 조건을 그대로 넘기고 결과를 돌려준다.")
    void findByQuery_delegates() {
        UUID userId = UUID.randomUUID();
        NotificationQuery query = NotificationQuery.of(null, null, 10);
        List<Notification> notifications = List.of();
        when(jpaRepository.findByQuery(userId, query)).thenReturn(notifications);

        assertThat(adapter.findByQuery(userId, query)).isSameAs(notifications);
    }

    @Test
    @DisplayName("개수 조회는 사용자 ID와 조건을 그대로 넘기고 개수를 돌려준다.")
    void countByQuery_delegates() {
        UUID userId = UUID.randomUUID();
        NotificationQuery query = NotificationQuery.of(null, null, 10);
        when(jpaRepository.countByQuery(userId, query)).thenReturn(3L);

        assertThat(adapter.countByQuery(userId, query)).isEqualTo(3L);
    }

    @Test
    @DisplayName("전체 확인은 사용자 ID와 확인 시각을 넘기고 바뀐 개수를 돌려준다.")
    void markAllAsRead_delegates() {
        UUID userId = UUID.randomUUID();
        LocalDateTime readAt = LocalDateTime.of(2026, 11, 6, 15, 0);
        when(jpaRepository.markAllAsRead(userId, readAt)).thenReturn(5);

        assertThat(adapter.markAllAsRead(userId, readAt)).isEqualTo(5);
    }

    @Test
    @DisplayName("기준 시각 이전 확인 알림 삭제는 기준 시각을 넘기고 지운 개수를 돌려준다.")
    void deleteReadBefore_delegates() {
        LocalDateTime threshold = LocalDateTime.of(2026, 10, 30, 0, 0);
        when(jpaRepository.deleteReadBefore(threshold)).thenReturn(2);

        assertThat(adapter.deleteReadBefore(threshold)).isEqualTo(2);
    }

    @Test
    @DisplayName("전체 조회는 JPA 리포지토리 결과를 그대로 돌려준다.")
    void findAll_delegates() {
        List<Notification> notifications = List.of();
        when(jpaRepository.findAll()).thenReturn(notifications);

        assertThat(adapter.findAll()).isSameAs(notifications);
    }

    @Test
    @DisplayName("ID로 삭제는 JPA 리포지토리에 그대로 넘긴다.")
    void deleteById_delegates() {
        UUID id = UUID.randomUUID();

        adapter.deleteById(id);

        verify(jpaRepository).deleteById(id);
    }
}
