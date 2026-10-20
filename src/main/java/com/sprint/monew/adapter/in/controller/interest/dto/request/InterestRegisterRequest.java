package com.sprint.monew.adapter.in.controller.interest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record InterestRegisterRequest(
        @NotBlank @Size(min = 1, max = NAME_MAX_LENGTH) String name, // 관심사 이름
        @NotEmpty @Size(max = KEYWORDS_MAX_SIZE) List<String> keywords) { // 관련 키워드 목록

    public static final int NAME_MAX_LENGTH = 50; // 관심사 이름 최대 글자 수
    public static final int KEYWORDS_MAX_SIZE = 10; // 키워드 최대 개수
}
