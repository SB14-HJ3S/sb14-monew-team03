package com.sprint.monew.global.type;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

// 알림이 가리키는 대상 종류
@Getter
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public enum ResourceType {
    @JsonProperty("interest")
    INTEREST, // 관심사 — 구독한 관심사에 새 기사가 등록됨
    @JsonProperty("comment")
    COMMENT // 댓글 — 내 댓글에 좋아요가 달림
}
