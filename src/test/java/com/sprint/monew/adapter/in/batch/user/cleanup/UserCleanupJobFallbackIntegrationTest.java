package com.sprint.monew.adapter.in.batch.user.cleanup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

import com.sprint.monew.adapter.out.repository.user.UserRepositoryAdapter;
import com.sprint.monew.domain.user.entity.User;
import com.sprint.monew.support.IntegrationTest;
import io.micrometer.core.instrument.MeterRegistry;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@IntegrationTest
class UserCleanupJobFallbackIntegrationTest {

    @MockitoSpyBean private UserRepositoryAdapter userRepository;

    @Autowired private JobLauncher jobLauncher;
    @Autowired private JobExplorer jobExplorer;
    @Autowired private MeterRegistry meterRegistry;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Autowired
    @Qualifier("userCleanupJob")
    private Job userCleanupJob;

    private final List<UUID> userIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        userIds.forEach(id -> jdbcTemplate.update("DELETE FROM users WHERE id = ?", id));
    }

    @Test
    @DisplayName("묶음 삭제가 실패해도 실제 UserUseCase가 한 명씩 삭제하고 Job과 삭제 집계가 성공한다")
    void completeJobWithIndividualDeletesWhenBulkDeleteRollsBack() throws Exception {
        User first = saveUserDeletedAgo("first", Duration.ofDays(2));
        User second = saveUserDeletedAgo("second", Duration.ofDays(2));
        User recent = saveUserDeletedAgo("recent", Duration.ofHours(1));
        User active = saveUser("active");
        double previousDeleted = deletedCounter();

        // 실제 묶음 삭제 후 실패시켜 서비스 트랜잭션의 롤백과 개별 재시도를 함께 검증한다.
        doAnswer(
                        invocation -> {
                            invocation.callRealMethod();
                            throw new DataAccessResourceFailureException("묶음 삭제 실패");
                        })
                .when(userRepository)
                .deleteUsers(anyList());

        JobExecution execution =
                jobLauncher.run(
                        userCleanupJob,
                        new JobParametersBuilder()
                                .addString("runId", UUID.randomUUID().toString())
                                .toJobParameters());

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(execution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
        assertThat(execution.getAllFailureExceptions()).isEmpty();
        assertThat(userRepository.findUserIncludingDeleted(first.getId())).isEmpty();
        assertThat(userRepository.findUserIncludingDeleted(second.getId())).isEmpty();
        assertThat(userRepository.findUserIncludingDeleted(recent.getId())).isPresent();
        assertThat(userRepository.findUser(active.getId())).isPresent();
        verify(userRepository).deleteUsers(anyList());
        verify(userRepository).deleteById(first.getId());
        verify(userRepository).deleteById(second.getId());
        assertThat(deletedCounter()).isEqualTo(previousDeleted + 2);

        JobExecution persisted = jobExplorer.getJobExecution(execution.getId());
        assertThat(persisted).isNotNull();
        assertThat(persisted.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(persisted.getStepExecutions())
                .singleElement()
                .satisfies(
                        step -> {
                            assertThat(step.getStepName()).isEqualTo("userCleanupStep");
                            assertThat(step.getStatus()).isEqualTo(BatchStatus.COMPLETED);
                            assertThat(step.getWriteCount()).isEqualTo(2);
                        });
    }

    private double deletedCounter() {
        return meterRegistry.get(UserCleanupMetrics.DELETED).counter().count();
    }

    private User saveUser(String nickname) {
        User user =
                userRepository.save(
                        User.create(UUID.randomUUID() + "@monew.com", nickname, "password1!"));
        userIds.add(user.getId());
        return user;
    }

    private User saveUserDeletedAgo(String nickname, Duration ago) {
        User user = saveUser(nickname);
        jdbcTemplate.update(
                "UPDATE users SET deleted_at = ? WHERE id = ?",
                Timestamp.from(Instant.now().minus(ago)),
                user.getId());
        return user;
    }
}
