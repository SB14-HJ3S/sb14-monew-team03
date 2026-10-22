package com.sprint.monew.adapter.in.controller.interest.dto.request;

import com.sprint.monew.domain.interest.entity.Interest;
import com.sprint.monew.domain.interest.entity.InterestKeyword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record InterestUpdateRequest(
        @NotEmpty @Size(max = Interest.MAX_KEYWORDS_SIZE)
                List<@NotBlank @Size(max = InterestKeyword.MAX_KEYWORD_LENGTH) String>
                        keywords) { // 수정 키워드 목록
}
