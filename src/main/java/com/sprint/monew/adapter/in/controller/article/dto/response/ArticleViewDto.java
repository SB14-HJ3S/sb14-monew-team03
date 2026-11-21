package com.sprint.monew.adapter.in.controller.article.dto.response;

import com.sprint.monew.application.article.provided.usecase.dto.ArticleViewStatusResult;
import com.sprint.monew.global.type.ArticleSourceType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

public record ArticleViewDto(
        @Schema(description = "기사 조회 ID") UUID id,
        @Schema(description = "기사를 조회한 사용자 ID") UUID viewedBy,
        @Schema(description = "기사를 본 날짜") Instant createdAt,
        @Schema(description = "기사 ID") UUID articleId,
        @Schema(description = "출처") ArticleSourceType source,
        @Schema(description = "원본 기사 URL") String sourceUrl,
        @Schema(description = "제목") String articleTitle,
        @Schema(description = "날짜") Instant articlePublishedDate,
        @Schema(description = "요약") String articleSummary,
        @Schema(description = "댓글 수") Long articleCommentCount,
        @Schema(description = "조회 수") Long articleViewCount) {
    public static ArticleViewDto from(ArticleViewStatusResult result) {
        return new ArticleViewDto(
                result.id(),
                result.viewedBy(),
                result.createdAt(),
                result.articleId(),
                result.source(),
                result.sourceUrl(),
                result.articleTitle(),
                result.articlePublishedDate().atZone(ZoneId.of("Asia/Seoul")).toInstant(),
                result.articleSummary(),
                result.articleCommentCount(),
                result.articleViewCount());
    }
}
