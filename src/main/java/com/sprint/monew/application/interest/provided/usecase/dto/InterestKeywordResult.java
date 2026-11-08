package com.sprint.monew.application.interest.provided.usecase.dto;

import com.sprint.monew.domain.interest.entity.Interest;
import java.util.List;

public record InterestKeywordResult(Interest interest, List<String> keywords) {

    public static InterestKeywordResult from(Interest interest) {
        return new InterestKeywordResult(interest, interest.getKeywordNames());
    }
}
