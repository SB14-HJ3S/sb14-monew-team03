package com.sprint.monew.application.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class NotificationApplicationUnitTest {
    @Mock private NotificationService notificationService;
    @InjectMocks private NotificationApplication notificationApplication;

    @Captor private ArgumentCaptor<LocalDateTime> thresholdCaptor;

    @Test
    @DisplayName("오래된 확인 알림 삭제 시 지금부터 7일 전을 기준 시각으로 넘기고 삭제 개수를 반환한다.")
    void whenDeleteOldReadNotifications_thenPassesSevenDaysAgo() {
        // given
        when(notificationService.deleteReadBefore(any(LocalDateTime.class))).thenReturn(3);
        LocalDateTime lowerBound = LocalDateTime.now().minusDays(7);

        // when
        int deleted = notificationApplication.deleteOldReadNotifications();

        // then
        LocalDateTime upperBound = LocalDateTime.now().minusDays(7);
        verify(notificationService).deleteReadBefore(thresholdCaptor.capture());

        assertThat(deleted).isEqualTo(3);
        assertThat(thresholdCaptor.getValue()).isBetween(lowerBound, upperBound);
    }
}
