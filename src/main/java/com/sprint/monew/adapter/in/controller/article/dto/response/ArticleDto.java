package com.sprint.monew.adapter.in.controller.article.dto.response;

import com.sprint.monew.application.article.provided.dto.ArticleResult;
import com.sprint.monew.global.type.ArticleSourceType;
import java.time.LocalDateTime;
import java.util.UUID;

public record ArticleDto(
        UUID id, // 기사 ID
        ArticleSourceType source, // 출처
        String sourceUrl, // 원본 기사 URL
        String title, // 제목
        LocalDateTime publishDate, // 날짜
        String summary, // 요약
        Long commentCount, // 댓글 수
        Long viewCount, // 조회 수
        boolean viewedByMe) {
    public static ArticleDto from(ArticleResult result) {
        return new ArticleDto(
                result.id(),
                result.source(),
                result.sourceUrl(),
                result.title(),
                result.publishedAt(),
                result.summary(),
                result.commentCount(),
                result.viewCount(),
                result.viewedByMe());
    }
}
