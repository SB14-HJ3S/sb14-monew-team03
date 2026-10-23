package com.sprint.monew.adapter.in.controller.user;

import com.sprint.monew.adapter.in.controller.user.dto.request.UserRegisterRequest;
import com.sprint.monew.adapter.in.controller.user.dto.response.UserDto;
import com.sprint.monew.application.user.provided.UserUseCase;
import com.sprint.monew.application.user.provided.dto.UserResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserUseCase userUseCase;

    @PostMapping
    public ResponseEntity<UserDto> register(@Valid @RequestBody UserRegisterRequest request) {
        log.info("회원가입 요청");
        UserResult result =
                userUseCase.register(request.email(), request.nickname(), request.password());
        log.info("회원가입 완료: userId={}", result.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(UserDto.from(result));
    }
}
