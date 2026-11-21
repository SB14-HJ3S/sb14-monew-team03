package com.sprint.monew.adapter.in.controller.article.dto.response;

import com.sprint.monew.application.article.provided.usecase.dto.ArticleResult;
import com.sprint.monew.global.type.ArticleSourceType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;

public record ArticleDto(
        @Schema(description = "기사 ID") UUID id,
        @Schema(description = "출처") ArticleSourceType source,
        @Schema(description = "원본 기사 URL") String sourceUrl,
        @Schema(description = "제목") String title,
        @Schema(description = "날짜") LocalDateTime publishDate,
        @Schema(description = "요약") String summary,
        @Schema(description = "댓글 수") Long commentCount,
        @Schema(description = "조회 수") Long viewCount,
        @Schema(description = "요청자의 조회 여부") boolean viewedByMe) {
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
