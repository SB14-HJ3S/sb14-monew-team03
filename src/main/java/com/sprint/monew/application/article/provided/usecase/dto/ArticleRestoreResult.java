package com.sprint.monew.application.article.provided.usecase.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ArticleRestoreResult(
        LocalDate restoreDate, List<UUID> restoredArticleIds, long restoredArticleCount) {}
