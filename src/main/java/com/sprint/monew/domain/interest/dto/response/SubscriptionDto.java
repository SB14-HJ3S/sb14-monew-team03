package com.sprint.monew.domain.interest.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SubscriptionDto(
        UUID id, // 구독 정보 ID
        UUID interestId, // 관심사 ID
        String interestName, // 관심사 이름
        List<String> interestKeywords, // 관련 키워드 목록
        Long interestSubscriberCount, // 구독자 수
        Instant createdAt) {} // 구독한 날짜
