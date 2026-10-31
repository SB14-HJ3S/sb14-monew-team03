package com.sprint.monew.adapter.in.batch.notification;

import com.sprint.monew.application.notification.provided.NotificationUseCase;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationCleanupTasklet implements Tasklet {

    private final NotificationUseCase useCase;

    @Override
    public @Nullable RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext)
            throws Exception {
        int deletedCount = useCase.deleteOldReadNotifications();
        contribution
                .getStepExecution()
                .getExecutionContext()
                .putInt(NotificationMetrics.DELETED, deletedCount);

        return RepeatStatus.FINISHED;
    }
}
