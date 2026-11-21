package com.sprint.monew.adapter.in.controller.useractivity;

import com.sprint.monew.adapter.in.controller.useractivity.dto.response.UserActivityDto;
import com.sprint.monew.application.useractivity.provided.usecase.UserActivityUseCase;
import com.sprint.monew.global.annotation.RestWebAdapter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;

@Tag(name = "사용자 활동 내역 관리", description = "사용자 활동 내역 관련 API")
@RestWebAdapter(path = "/api/user-activities")
@RequiredArgsConstructor
public class UserActivityController {

    private final UserActivityUseCase userActivityUseCase;

    @Operation(summary = "사용자 활동 내역 조회", description = "사용자 ID로 활동 내역을 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 (사용자 ID 형식 오류)"),
        @ApiResponse(responseCode = "404", description = "사용자 정보 없음 (없거나 탈퇴한 사용자)")
    })
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/{userId}")
    public UserActivityDto findUserActivity(
            @Parameter(description = "사용자 ID", required = true) @PathVariable UUID userId) {
        return UserActivityDto.from(userActivityUseCase.findByUserId(userId));
    }
}
