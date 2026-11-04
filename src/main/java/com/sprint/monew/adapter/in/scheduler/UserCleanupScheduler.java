package com.sprint.monew.adapter.in.scheduler;

import com.sprint.monew.adapter.in.batch.UserCleanupRunner;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "monew.scheduling.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class UserCleanupScheduler {

    private final UserCleanupRunner runner;

    @Scheduled(cron = "${monew.user.cleanup.cron}", zone = "Asia/Seoul")
    public void cleanup() {
        runner.runCleanup();
    }
}
