package com.sprint.monew.adapter.in.controller.article.dto.response;

import com.sprint.monew.application.article.provided.dto.ArticleRestoreResult;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

public record ArticleRestoreResultDto(
        Instant restoreDate, List<UUID> restoredArticleIds, Long restoredArticleCount) {

    public static ArticleRestoreResultDto from(ArticleRestoreResult result) {
        return new ArticleRestoreResultDto(
                result.restoreDate().atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant(),
                result.restoredArticleIds(),
                result.restoredArticleCount());
    }
}
