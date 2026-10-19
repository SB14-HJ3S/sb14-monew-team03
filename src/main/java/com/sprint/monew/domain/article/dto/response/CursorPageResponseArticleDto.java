package com.sprint.monew.domain.article.dto.response;

import java.time.Instant;
import java.util.List;

public record CursorPageResponseArticleDto(
        List<ArticleDto> content, // 페이지 내용
        String nextCursor, // 다음 페이지 커서
        Instant nextAfter, // 다음 보조 커서(마지막 요소의 생성 시간)
        int size, // 페이지 크기
        long totalElements, // 총 요소 수
        boolean hasNext) {} // 다음 페이지 여부
