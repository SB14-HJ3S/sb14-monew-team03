package com.sprint.monew.adapter.in.controller.interest;

import com.sprint.monew.adapter.in.controller.interest.dto.request.InterestRegisterRequest;
import com.sprint.monew.adapter.in.controller.interest.dto.request.InterestUpdateRequest;
import com.sprint.monew.adapter.in.controller.interest.dto.response.InterestDto;
import com.sprint.monew.adapter.in.controller.interest.dto.response.SubscriptionDto;
import com.sprint.monew.application.interest.provided.usecase.dto.InterestQuery;
import com.sprint.monew.global.pagination.PageResult;
import com.sprint.monew.global.pagination.SortDirection;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.util.UUID;

@Tag(name = "관심사 관리", description = "관심사 관련 API")
public interface InterestController {
    @Operation(summary = "관심사 등록", description = "새로운 관심사를 등록합니다.")
    @ApiResponse(responseCode = "201", description = "등록 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청 (입력값 검증 실패)")
    @ApiResponse(responseCode = "409", description = "유사 관심사 중복")
    @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    InterestDto registerNewInterest(@Valid InterestRegisterRequest request);

    @Operation(summary = "관심사 목록 조회", description = "조건에 맞는 관심사 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청 (정렬 기준 오류, 페이지네이션 파라미터 오류 등)")
    @ApiResponse(responseCode = "404", description = "커서에 해당하는 관심사 정보 없음")
    @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    PageResult<InterestDto> findInterestPagesByQuery(
            @Parameter(description = "검색어(관심사 이름, 키워드)", example = "스포츠") String keyword,
            @Parameter(
                            name = "orderBy",
                            description = "정렬 속성 이름",
                            required = true,
                            schema =
                                    @Schema(
                                            implementation = String.class,
                                            allowableValues = {"name", "subscriberCount"}))
                    InterestQuery.SortOrder sortOrder,
            @Parameter(name = "direction", description = "정렬 방향 (ASC, DESC)", required = true)
                    SortDirection sortDirection,
            @Parameter(description = "커서 값") String cursor,
            @Parameter(description = "보조 커서(createdAt) 값") Instant after,
            @Parameter(description = "커서 페이지 크기", required = true, example = "50")
                    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다")
                    int limit);

    @Operation(summary = "관심사 정보 수정", description = "관심사의 키워드를 수정합니다.")
    @ApiResponse(responseCode = "200", description = "수정 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청 (입력값 검증 실패)")
    @ApiResponse(responseCode = "404", description = "관심사 정보 없음")
    @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    InterestDto updateKeyword(
            @Parameter(description = "관심사 ID") UUID interestId,
            @Valid InterestUpdateRequest request);

    @Operation(summary = "관심사 물리 삭제", description = "관심사를 물리적으로 삭제합니다.")
    @ApiResponse(responseCode = "204", description = "삭제 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청 (관심사 ID 형식 오류)")
    @ApiResponse(responseCode = "404", description = "관심사 정보 없음")
    @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    void deleteInterest(@Parameter(description = "관심사 ID") UUID interestId);

    @Operation(summary = "관심사 구독", description = "관심사를 구독합니다.")
    @ApiResponse(responseCode = "200", description = "구독 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청 (요청자 헤더 또는 관심사 ID 형식 오류)")
    @ApiResponse(responseCode = "404", description = "관심사 정보 없음")
    @ApiResponse(responseCode = "409", description = "이미 구독 중인 관심사")
    @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    SubscriptionDto subscribe(@Parameter(description = "관심사 ID") UUID interestId);

    @Operation(summary = "관심사 구독 취소", description = "관심사를 구독을 취소합니다.")
    @ApiResponse(responseCode = "200", description = "구독 취소 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청 (요청자 헤더 또는 관심사 ID 형식 오류)")
    @ApiResponse(responseCode = "404", description = "관심사 정보 없음")
    @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    void unsubscribe(@Parameter(description = "관심사 ID") UUID interestId);
}
