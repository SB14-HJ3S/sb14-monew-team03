package com.sprint.monew.domain.interest.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record InterestUpdateRequest(
        @NotEmpty @Size(max = KEYWORDS_MAX_SIZE) List<String> keywords) { // 수정 키워드 목록

    public static final int KEYWORDS_MAX_SIZE = 10; // 키워드 최대 개수
}
