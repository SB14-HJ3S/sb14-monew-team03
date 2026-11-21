package com.sprint.monew.global.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class AsyncConfigTest {

    @Test
    @DisplayName("활동 내역 실행기는 스레드 1개로 이벤트를 순서대로 처리한다.")
    void userActivityExecutor_isSingleThread() {
        ThreadPoolTaskExecutor executor = new AsyncConfig().userActivityExecutor();

        assertThat(executor.getCorePoolSize()).isEqualTo(1);
        assertThat(executor.getMaxPoolSize()).isEqualTo(1);
        assertThat(executor.getThreadNamePrefix()).isEqualTo("user-activity-");

        executor.shutdown();
    }
}
