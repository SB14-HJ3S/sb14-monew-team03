package com.sprint.monew.adapter.in.controller.article.dto.response;

import com.sprint.monew.global.type.ArticleSourceType;
import java.time.Instant;
import java.util.UUID;

public record ArticleViewDto(
        UUID id, // 기사 조회 ID
        UUID viewedBy, // 기사를 조회한 사용자 ID
        Instant createdAt, // 기사를 본 날짜
        UUID articleId, // 기사 ID
        ArticleSourceType source, // 출처
        String sourceUrl, // 원본 기사 URL
        String articleTitle, // 제목
        Instant articlePublishedDate, // 날짜
        String articleSummary, // 요약
        Long articleCommentCount, // 댓글 수
        Long articleViewCount) {} // 조회 수
