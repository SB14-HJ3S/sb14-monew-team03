package com.sprint.monew.adapter.in.controller.interest.dto.request;

import com.sprint.monew.domain.interest.entity.Interest;
import com.sprint.monew.domain.interest.entity.InterestKeyword;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "관심사 등록 요청")
public record InterestRegisterRequest(
        @Schema(description = "관심사 이름", example = "스포츠")
                @NotBlank
                @Size(min = 1, max = Interest.MAX_NAME_LENGTH)
                String name,
        @Schema(description = "관련 키워드 목록", example = "[\"축구\", \"야구\"]")
                @NotEmpty
                @Size(min = 1, max = Interest.MAX_KEYWORDS_SIZE)
                List<@NotBlank @Size(max = InterestKeyword.MAX_KEYWORD_LENGTH) String> keywords) {}
