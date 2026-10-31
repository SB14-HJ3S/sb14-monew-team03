package com.sprint.monew.adapter.in.controller.user;

import com.sprint.monew.adapter.in.controller.user.dto.request.UserLoginRequest;
import com.sprint.monew.adapter.in.controller.user.dto.request.UserRegisterRequest;
import com.sprint.monew.adapter.in.controller.user.dto.request.UserUpdateRequest;
import com.sprint.monew.adapter.in.controller.user.dto.response.UserDto;
import com.sprint.monew.application.user.provided.UserUseCase;
import com.sprint.monew.application.user.provided.dto.UserResult;
import com.sprint.monew.global.annotation.RestWebAdapter;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @PostMapping("/login")
    public UserDto login(@Valid @RequestBody UserLoginRequest request) {
        log.info("로그인 요청");
        UserResult result = userUseCase.login(request.email(), request.password());
        log.info("로그인 성공: userId={}", result.id());
        return UserDto.from(result);
    }

    @PatchMapping("/{userId}")
    public UserDto updateNickname(
            @PathVariable UUID userId, @Valid @RequestBody UserUpdateRequest request) {
        log.info("닉네임 수정 요청: userId={}", userId);
        UserResult result = userUseCase.updateNickname(userId, request.nickname());
        log.info("닉네임 수정 완료: userId={}", userId);
        return UserDto.from(result);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{userId}")
    public void softDeleteUser(@PathVariable UUID userId) {
        log.info("사용자 논리 삭제 요청: userId={}", userId);
        userUseCase.softDeleteUser(userId);
        log.info("사용자 논리 삭제 완료: userId={}", userId);
    }

    @DeleteMapping("/{userId}/hard")
    public void hardDeleteUser(@PathVariable UUID userId) {}
}
