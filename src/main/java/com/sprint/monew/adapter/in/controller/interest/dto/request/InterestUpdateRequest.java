package com.sprint.monew.adapter.in.controller.interest.dto.request;

import com.sprint.monew.domain.interest.entity.Interest;
import com.sprint.monew.domain.interest.entity.InterestKeyword;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "관심사 키워드 수정 요청")
public record InterestUpdateRequest(
        @Schema(description = "수정할 키워드 목록", example = "[\"축구\", \"농구\"]")
                @NotEmpty
                @Size(max = Interest.MAX_KEYWORDS_SIZE)
                List<@NotBlank @Size(max = InterestKeyword.MAX_KEYWORD_LENGTH) String> keywords) {}
