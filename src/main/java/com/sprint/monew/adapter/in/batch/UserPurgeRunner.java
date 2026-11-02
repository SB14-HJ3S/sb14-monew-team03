package com.sprint.monew.adapter.in.batch;

import com.sprint.monew.application.user.provided.UserUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserPurgeRunner {

    private final UserUseCase userUseCase;

    public void runPurge() {}
}
