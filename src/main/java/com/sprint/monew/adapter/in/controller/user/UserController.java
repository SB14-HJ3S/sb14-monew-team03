package com.sprint.monew.adapter.in.controller.user;

import com.sprint.monew.adapter.in.controller.user.dto.request.UserLoginRequest;
import com.sprint.monew.adapter.in.controller.user.dto.request.UserRegisterRequest;
import com.sprint.monew.adapter.in.controller.user.dto.request.UserUpdateRequest;
import com.sprint.monew.adapter.in.controller.user.dto.response.UserDto;
import com.sprint.monew.application.user.provided.UserUseCase;
import com.sprint.monew.application.user.provided.dto.UserResult;
import com.sprint.monew.global.annotation.RestWebAdapter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "사용자 관리", description = "사용자 관련 API")
@RestWebAdapter(path = "/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserUseCase userUseCase;

    @Operation(summary = "회원가입")
    @ApiResponse(responseCode = "201", description = "회원가입 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청 (입력값 검증 실패)")
    @ApiResponse(responseCode = "409", description = "이메일 중복")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public UserDto register(@Valid @RequestBody UserRegisterRequest request) {
        log.info("회원가입 요청");
        UserResult result =
                userUseCase.register(request.email(), request.nickname(), request.password());
        log.info("회원가입 완료: userId={}", result.id());
        return UserDto.from(result);
    }

    @Operation(summary = "로그인")
    @ApiResponse(responseCode = "200", description = "로그인 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청 (입력값 검증 실패)")
    @ApiResponse(responseCode = "401", description = "로그인 실패 (이메일 또는 비밀번호 불일치)")
    @PostMapping("/login")
    public UserDto login(@Valid @RequestBody UserLoginRequest request) {
        log.info("로그인 요청");
        UserResult result = userUseCase.login(request.email(), request.password());
        log.info("로그인 성공: userId={}", result.id());
        return UserDto.from(result);
    }

    @Operation(summary = "사용자 정보 수정")
    @ApiResponse(responseCode = "200", description = "사용자 정보 수정 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청 (입력값 검증 실패)")
    @ApiResponse(responseCode = "404", description = "사용자 정보 없음")
    @PatchMapping("/{userId}")
    public UserDto updateNickname(
            @Parameter(description = "사용자 ID") @PathVariable UUID userId,
            @Valid @RequestBody UserUpdateRequest request) {
        log.info("닉네임 수정 요청: userId={}", userId);
        UserResult result = userUseCase.updateNickname(userId, request.nickname());
        log.info("닉네임 수정 완료: userId={}", userId);
        return UserDto.from(result);
    }

    @Operation(summary = "사용자 논리 삭제")
    @ApiResponse(responseCode = "204", description = "사용자 삭제 성공")
    @ApiResponse(responseCode = "404", description = "사용자 정보 없음")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{userId}")
    public void softDeleteUser(@Parameter(description = "사용자 ID") @PathVariable UUID userId) {
        log.info("사용자 논리 삭제 요청: userId={}", userId);
        userUseCase.softDeleteUser(userId);
        log.info("사용자 논리 삭제 완료: userId={}", userId);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{userId}/hard")
    public void hardDeleteUser(@PathVariable UUID userId) {
        log.info("사용자 물리 삭제 요청: userId={}", userId);
        userUseCase.hardDeleteUser(userId);
        log.info("사용자 물리 삭제 완료: userId={}", userId);
    }
}
