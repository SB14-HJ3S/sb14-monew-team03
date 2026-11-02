package com.sprint.monew.adapter.in.scheduler;

import com.sprint.monew.adapter.in.batch.NotificationCleanupRunner;
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
public class NotificationCleanupScheduler {

    private final NotificationCleanupRunner runner;

    @Scheduled(cron = "${monew.notification.cleanup.cron}", zone = "Asia/Seoul")
    public void cleanup() {
        runner.runCleanup();
    }
}
