package com.sprint.monew.adapter.in.controller.useractivity;

import com.sprint.monew.adapter.in.controller.useractivity.dto.response.UserActivityDto;
import com.sprint.monew.application.useractivity.provided.UserActivityUseCase;
import com.sprint.monew.global.annotation.RestWebAdapter;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;

@RestWebAdapter(path = "/api/user-activities")
@RequiredArgsConstructor
public class UserActivityController {

    private final UserActivityUseCase userActivityUseCase;

    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/{userId}")
    public UserActivityDto findUserActivity(@PathVariable UUID userId) {
        return UserActivityDto.from(userActivityUseCase.findByUserId(userId));
    }
}
