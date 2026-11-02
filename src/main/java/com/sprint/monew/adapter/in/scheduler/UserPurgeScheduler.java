package com.sprint.monew.adapter.in.scheduler;

import com.sprint.monew.adapter.in.batch.UserPurgeRunner;
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
public class UserPurgeScheduler {

    private final UserPurgeRunner runner;

    @Scheduled(cron = "${monew.user.purge.cron}", zone = "Asia/Seoul")
    public void purge() {
        runner.runPurge();
    }
}
