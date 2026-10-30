package com.sprint.monew.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.monew.support.IntegrationTest;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.test.context.TestPropertySource;

@IntegrationTest
@TestPropertySource(properties = "monew.scheduling.enabled=true")
@Import(ScheduledLockAspectTest.FakeBatchConfig.class)
class ScheduledLockAspectTest {

    @Autowired FakeBatch fakeBatch;
    @Autowired JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update(
                "UPDATE shedlock SET lock_until = timezone('utc', now()) - interval '1 second'");
        fakeBatch.reset();
    }

    @Test
    @DisplayName("같은 배치를 동시에 두 번 실행해도 한 번만 돌고, shedlock 표에 잠금 이름이 남는다")
    void runsOnceWhenCalledConcurrently() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        for (int i = 0; i < 2; i++) {
            pool.submit(
                    () -> {
                        start.await();
                        fakeBatch.run();
                        return null;
                    });
        }
        start.countDown();
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);

        assertThat(fakeBatch.count()).isEqualTo(1);
        assertThat(
                        jdbcTemplate.queryForObject(
                                "SELECT name FROM shedlock WHERE name LIKE '%FakeBatch.run'",
                                String.class))
                .isEqualTo(FakeBatch.class.getName() + ".run");
    }

    @Test
    @DisplayName("끝난 직후 다시 실행해도 최소 잠금 시간(30초) 동안은 건너뛴다")
    void skipsWithinLockAtLeastFor() throws Exception {
        fakeBatch.run();
        fakeBatch.run();

        assertThat(fakeBatch.count()).isEqualTo(1);
    }

    @TestConfiguration
    static class FakeBatchConfig {
        @Bean
        FakeBatch fakeBatch() {
            return new FakeBatch();
        }
    }

    static class FakeBatch {
        private final AtomicInteger count = new AtomicInteger();

        @Scheduled(cron = "0 0 0 1 1 *")
        public void run() throws InterruptedException {
            count.incrementAndGet();
            Thread.sleep(500);
        }

        public int count() {
            return count.get();
        }

        public void reset() {
            count.set(0);
        }
    }
}
