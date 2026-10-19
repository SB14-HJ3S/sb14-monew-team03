package com.sprint.monew.domain.article.dto.response;

import com.sprint.monew.global.type.ArticleSourceType;
import java.time.Instant;
import java.util.UUID;

public record ArticleDto(
        UUID id, // 기사 ID
        ArticleSourceType source, // 출처
        String sourceUrl, // 원본 기사 URL
        String title, // 제목
        Instant publishDate, // 날짜
        String summary, // 요약
        Long commentCount, // 댓글 수
        Long viewCount, // 조회 수
        boolean viewedByMe) {} // 요청자의 조회 여부
