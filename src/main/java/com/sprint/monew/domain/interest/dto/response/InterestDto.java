package com.sprint.monew.domain.interest.dto.response;

import java.util.List;
import java.util.UUID;

public record InterestDto(
        UUID id, // 관심사 ID
        String name, // 관심사 이름
        List<String> keywords, // 관련 키워드 목록
        Long subscriberCount, // 구독자 수
        boolean subscribedByMe) {} // 요청자의 구독 여부
