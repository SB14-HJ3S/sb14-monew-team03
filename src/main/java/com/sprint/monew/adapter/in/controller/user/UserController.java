package com.sprint.monew.adapter.in.controller.user;

import com.sprint.monew.adapter.in.controller.user.dto.request.UserRegisterRequest;
import com.sprint.monew.adapter.in.controller.user.dto.response.UserDto;
import com.sprint.monew.application.user.provided.UserUseCase;
import com.sprint.monew.application.user.provided.dto.UserResult;
import com.sprint.monew.global.annotation.RestWebAdapter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Slf4j
@RestWebAdapter(path = "/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserUseCase userUseCase;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public UserDto register(@Valid @RequestBody UserRegisterRequest request) {
        log.info("회원가입 요청");
        UserResult result =
                userUseCase.register(request.email(), request.nickname(), request.password());
        log.info("회원가입 완료: userId={}", result.id());
        return UserDto.from(result);
    }
}
