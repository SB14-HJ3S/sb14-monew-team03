package com.sprint.monew.application.interest.provided.dto;

import com.sprint.monew.domain.interest.entity.Interest;
import com.sprint.monew.domain.interest.entity.InterestKeyword;
import java.util.List;
import java.util.UUID;

public record InterestResult(
        UUID id, String name, List<String> keywords, Long subscriberCount, Boolean subscribedByMe) {
    public static InterestResult of(Interest interest, Boolean isSubscribed) {
        return new InterestResult(
                interest.getId(),
                interest.getName(),
                interest.getKeywords().stream().map(InterestKeyword::getKeyword).toList(),
                interest.getSubscriberCount(),
                isSubscribed);
    }
}
