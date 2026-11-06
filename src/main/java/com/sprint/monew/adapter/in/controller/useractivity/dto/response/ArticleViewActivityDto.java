package com.sprint.monew.adapter.in.controller.useractivity.dto.response;

import com.sprint.monew.application.useractivity.provided.dto.ArticleViewActivityResult;
import com.sprint.monew.global.type.ArticleSourceType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "최근 본 뉴스 기사")
public record ArticleViewActivityDto(
        @Schema(description = "기사 조회 ID") UUID id,
        @Schema(description = "조회한 사용자 ID") UUID viewedBy,
        @Schema(description = "본 날짜") Instant createdAt,
        @Schema(description = "기사 ID") UUID articleId,
        @Schema(description = "출처", example = "NAVER") ArticleSourceType source,
        @Schema(description = "원본 기사 링크") String sourceUrl,
        @Schema(description = "기사 제목") String articleTitle,
        @Schema(description = "기사 날짜") Instant articlePublishedDate,
        @Schema(description = "기사 요약") String articleSummary,
        @Schema(description = "댓글 수") long articleCommentCount,
        @Schema(description = "조회 수") long articleViewCount) {
    public static ArticleViewActivityDto from(ArticleViewActivityResult r) {
        return new ArticleViewActivityDto(
                r.id(),
                r.viewedBy(),
                r.createdAt(),
                r.articleId(),
                r.source(),
                r.sourceUrl(),
                r.articleTitle(),
                r.articlePublishedDate(),
                r.articleSummary(),
                r.articleCommentCount(),
                r.articleViewCount());
    }
}
