package com.sprint.monew.adapter.in.controller.article;

import com.sprint.monew.adapter.in.controller.article.dto.response.ArticleDto;
import com.sprint.monew.adapter.in.controller.article.dto.response.ArticleViewDto;
import com.sprint.monew.application.article.provided.ArticleUseCase;
import com.sprint.monew.application.article.provided.dto.ArticleQuery;
import com.sprint.monew.application.article.provided.dto.ArticleResult;
import com.sprint.monew.global.annotation.RequestId;
import com.sprint.monew.global.annotation.RestWebAdapter;
import com.sprint.monew.global.pagination.PageResult;
import com.sprint.monew.global.pagination.SortDirection;
import com.sprint.monew.global.type.ArticleSourceType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "뉴스 기사 관리", description = "뉴스 기사 관련 API")
@RestWebAdapter(path = "/api/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleUseCase articleUseCase;

    @Operation(summary = "출처 목록 조회", description = "출처 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/sources")
    public List<ArticleSourceType> findSourceTypes() {
        return articleUseCase.findSourceTypes();
    }

    @RequestId
    @GetMapping("/{articleId}")
    public ArticleDto findById(@PathVariable UUID articleId) {
        return ArticleDto.from(articleUseCase.findById(articleId));
    }

    @RequestId
    @GetMapping
    public PageResult<ArticleDto> findArticlePagesByQuery(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "interestId", required = false) UUID interestId,
            @RequestParam(name = "sourceIn", required = false) List<ArticleSourceType> sourceIn,
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
            @RequestParam(name = "limit") @Min(1) int limit) {

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
                        limit);

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

    @RequestId
    @PostMapping("/{articleId}/article-views")
    public ArticleViewDto registerView(@PathVariable UUID articleId) {
        return ArticleViewDto.from(articleUseCase.register(articleId));
    }
}
