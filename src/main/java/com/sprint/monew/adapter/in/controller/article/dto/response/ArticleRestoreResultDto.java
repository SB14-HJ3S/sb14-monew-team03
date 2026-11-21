package com.sprint.monew.adapter.in.controller.article.dto.response;

import com.sprint.monew.application.article.provided.usecase.dto.ArticleRestoreResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

public record ArticleRestoreResultDto(
        @Schema(description = "날짜") Instant restoreDate,
        @Schema(description = "복구된 기사 ID 목록") List<UUID> restoredArticleIds,
        @Schema(description = "복구된 기사 수") Long restoredArticleCount) {

    public static ArticleRestoreResultDto from(ArticleRestoreResult result) {
        return new ArticleRestoreResultDto(
                result.restoreDate().atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant(),
                result.restoredArticleIds(),
                result.restoredArticleCount());
    }
}
