package com.sprint.monew.adapter.in.controller.article;

import com.sprint.monew.adapter.in.controller.article.dto.response.ArticleDto;
import com.sprint.monew.application.article.provided.ArticleUseCase;
import com.sprint.monew.application.article.provided.dto.ArticleQuery;
import com.sprint.monew.application.article.provided.dto.ArticleResult;
import com.sprint.monew.global.annotation.RestWebAdapter;
import com.sprint.monew.global.pagination.PageResult;
import com.sprint.monew.global.pagination.SortDirection;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestWebAdapter(path = "/api/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleUseCase articleUseCase;

    @GetMapping
    public PageResult<ArticleDto> findArticlePagesByQuery(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "interestId", required = false) UUID interestId,
            @RequestParam(name = "sourceIn", required = false) List<String> sourceIn,
            @RequestParam(name = "publishDateFrom", required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime publishDateFrom,
            @RequestParam(name = "publishDateTo", required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime publishDateTo,
            @RequestParam(name = "orderBy") ArticleQuery.SortOrder orderBy,
            @RequestParam(name = "direction") SortDirection direction,
            @RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "after", required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant after,
            @RequestParam(name = "limit") @Min(1) int limit,
            @RequestHeader("Monew-Request-User-ID") UUID requestId) {

        ArticleQuery query =
                new ArticleQuery(
                        keyword,
                        interestId,
                        sourceIn,
                        publishDateFrom,
                        publishDateTo,
                        orderBy,
                        direction,
                        cursor,
                        after,
                        limit,
                        requestId);

        PageResult<ArticleResult> result = articleUseCase.findArticlePage(query);

        List<ArticleDto> content = result.content().stream().map(ArticleDto::from).toList();

        return new PageResult<>(
                content,
                result.nextCursor(),
                result.nextAfter(),
                result.size(),
                result.totalElements(),
                result.hasNext());
    }
}
