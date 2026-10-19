package com.sprint.monew.domain.article.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ArticleRestoreResultDto(
        Instant restoreDate, // 날짜
        List<UUID> restoredArticleIds, // 복구된 기사 ID 목록
        Long restoredArticleCount) {} // 복구된 기사 수
