package com.sprint.monew.global.config;

import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import net.javacrumbs.shedlock.core.LockingTaskExecutor.TaskResult;
import net.javacrumbs.shedlock.core.LockingTaskExecutor.TaskWithResult;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "monew.scheduling.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class ScheduledLockAspect {

    private static final Duration LOCK_AT_MOST_FOR = Duration.ofMinutes(50);
    private static final Duration LOCK_AT_LEAST_FOR = Duration.ofSeconds(30);

    private final LockingTaskExecutor lockingTaskExecutor;

    @Around("@annotation(org.springframework.scheduling.annotation.Scheduled)")
    public Object runWithLock(ProceedingJoinPoint joinPoint) throws Throwable {
        String lockName =
                joinPoint.getSignature().getDeclaringTypeName()
                        + "."
                        + joinPoint.getSignature().getName();

        TaskWithResult<Object> task = joinPoint::proceed;
        TaskResult<Object> result =
                lockingTaskExecutor.executeWithLock(
                        task,
                        new LockConfiguration(
                                Instant.now(), lockName, LOCK_AT_MOST_FOR, LOCK_AT_LEAST_FOR));

        if (!result.wasExecuted()) {
            log.info("[배치 건너뜀] 다른 앱이 이미 실행 중: {}", lockName);
        }
        return null;
    }
}
