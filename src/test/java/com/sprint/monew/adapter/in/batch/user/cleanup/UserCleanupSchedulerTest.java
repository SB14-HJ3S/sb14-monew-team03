package com.sprint.monew.adapter.in.batch.user.cleanup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;

@ExtendWith(MockitoExtension.class)
class UserCleanupSchedulerTest {

    @Mock private JobLauncher jobLauncher;
    @Mock private Job userCleanupJob;

    @Test
    @DisplayName("같은 날의 매시간 실행에 서로 다른 식별 파라미터를 전달한다")
    void useDistinctIdentifyingParametersForHourlyExecutions() throws Exception {
        ZoneId zone = ZoneId.of("Asia/Seoul");
        LocalDateTime firstTime = LocalDateTime.of(2026, 11, 4, 1, 0);
        LocalDateTime secondTime = firstTime.plusHours(1);
        UserCleanupScheduler scheduler = new UserCleanupScheduler(jobLauncher, userCleanupJob);

        try (MockedStatic<LocalDateTime> clock = mockStatic(LocalDateTime.class)) {
            clock.when(() -> LocalDateTime.now(zone)).thenReturn(firstTime, secondTime);

            scheduler.cleanup();
            scheduler.cleanup();
        }

        ArgumentCaptor<JobParameters> captor = ArgumentCaptor.forClass(JobParameters.class);
        verify(jobLauncher, times(2)).run(eq(userCleanupJob), captor.capture());
        List<JobParameters> parameters = captor.getAllValues();
        assertThat(parameters.get(0).getLocalDateTime("dateTime")).isEqualTo(firstTime);
        assertThat(parameters.get(1).getLocalDateTime("dateTime")).isEqualTo(secondTime);
        assertThat(parameters.get(0).getIdentifyingParameters())
                .isNotEqualTo(parameters.get(1).getIdentifyingParameters());
    }
}
