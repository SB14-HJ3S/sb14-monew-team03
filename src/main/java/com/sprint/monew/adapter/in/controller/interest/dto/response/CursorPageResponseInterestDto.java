package com.sprint.monew.adapter.in.controller.interest.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

public record CursorPageResponseInterestDto(
        @Schema(description = "페이지 내용") List<InterestDto> content,
        @Schema(description = "다음 페이지 커서(다음 페이지가 없으면 null)") String nextCursor,
        @Schema(description = "다음 보조 커서(마지막 항목의 생성 시각, 다음 페이지가 없으면 null)") Instant nextAfter,
        @Schema(description = "현재 페이지의 항목 수") int size,
        @Schema(description = "검색 조건에 일치하는 전체 항목 수") long totalElements,
        @Schema(description = "다음 페이지 존재 여부") boolean hasNext) {}
