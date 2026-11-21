package com.sprint.monew.adapter.in.batch.user.cleanup;

import com.sprint.monew.application.user.provided.usecase.UserUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserCleanupTasklet implements Tasklet {

    private final UserUseCase userUseCase;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext)
            throws Exception {
        int deletedCount = userUseCase.cleanupDeletedUsers();
        contribution.incrementWriteCount(deletedCount);
        return RepeatStatus.FINISHED;
    }
}
