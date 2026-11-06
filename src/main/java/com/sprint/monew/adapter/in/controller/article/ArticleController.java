package com.sprint.monew.adapter.in.controller.article;

import com.sprint.monew.adapter.in.controller.article.dto.response.ArticleDto;
import com.sprint.monew.adapter.in.controller.article.dto.response.ArticleRestoreResultDto;
import com.sprint.monew.adapter.in.controller.article.dto.response.ArticleViewDto;
import com.sprint.monew.application.article.provided.ArticleRestoreUseCase;
import com.sprint.monew.application.article.provided.ArticleUseCase;
import com.sprint.monew.application.article.provided.dto.ArticleQuery;
import com.sprint.monew.application.article.provided.dto.ArticleResult;
import com.sprint.monew.global.annotation.RequestId;
import com.sprint.monew.global.annotation.RestWebAdapter;
import com.sprint.monew.global.pagination.PageResult;
import com.sprint.monew.global.pagination.SortDirection;
import com.sprint.monew.global.type.ArticleSourceType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

@Slf4j
@Tag(name = "뉴스 기사 관리", description = "뉴스 기사 관련 API")
@RestWebAdapter(path = "/api/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleUseCase articleUseCase;
    private final ArticleRestoreUseCase articleRestoreUseCase;

    @Operation(summary = "뉴스 복구", description = "유실된 뉴스 기사를 복구.")
    @ApiResponse(responseCode = "200", description = "복구 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청 (날짜 누락, 시작 날짜가 끝 날짜보다 늦음, 최대 31일 초과)")
    @GetMapping("/restore")
    public List<ArticleRestoreResultDto> restoreArticles(
            @Parameter(description = "날짜 시작(범위)")
                    @RequestParam(name = "from")
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime from,
            @Parameter(description = "날짜 끝(범위)")
                    @RequestParam(name = "to")
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime to) {
        return articleRestoreUseCase.restoreArticles(from, to).stream()
                .map(ArticleRestoreResultDto::from)
                .toList();
    }

    @Operation(summary = "출처 목록 조회", description = "출처 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/sources")
    public List<ArticleSourceType> findSourceTypes() {
        return articleUseCase.findSourceTypes();
    }

    @Operation(summary = "뉴스 기사 단건 조회", description = "뉴스 기사 ID로 뉴스 기사 단건을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "404", description = "뉴스 기사 정보 없음")
    @RequestId
    @GetMapping("/{articleId}")
    public ArticleDto findById(@Parameter(description = "뉴스 기사 ID") @PathVariable UUID articleId) {
        return ArticleDto.from(articleUseCase.findById(articleId));
    }

    @Operation(summary = "뉴스 기사 목록 조회", description = "조건에 맞는 뉴스 기사 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청 (정렬 기준 오류, 페이지네이션 파라미터 오류 등)")
    @RequestId
    @GetMapping
    public PageResult<ArticleDto> findArticlePagesByQuery(
            @Parameter(description = "검색어(제목, 요약)")
                    @RequestParam(name = "keyword", required = false)
                    String keyword,
            @Parameter(description = "관심사 ID") @RequestParam(name = "interestId", required = false)
                    UUID interestId,
            @Parameter(description = "출처(포함)") @RequestParam(name = "sourceIn", required = false)
                    List<ArticleSourceType> sourceIn,
            @Parameter(description = "날짜 시작(범위) (LocalDateTime)")
                    @RequestParam(name = "publishDateFrom", required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime publishDateFrom,
            @Parameter(description = "날짜 끝(범위) (LocalDateTime)")
                    @RequestParam(name = "publishDateTo", required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime publishDateTo,
            @Parameter(description = "정렬 속성 이름") @RequestParam(name = "orderBy")
                    ArticleQuery.SortOrder orderBy,
            @Parameter(description = "정렬 방향 (ASC, DESC)") @RequestParam(name = "direction")
                    SortDirection direction,
            @Parameter(description = "커서 값") @RequestParam(name = "cursor", required = false)
                    String cursor,
            @Parameter(description = "보조 커서(createdAt) 값")
                    @RequestParam(name = "after", required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant after,
            @Parameter(description = "커서 페이지 크기") @RequestParam(name = "limit") @Min(1) int limit) {

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

    @Operation(summary = "뉴스 기사 논리 삭제", description = "뉴스 기사를 논리적으로 삭제합니다.")
    @ApiResponse(responseCode = "204", description = "논리 삭제 성공")
    @ApiResponse(responseCode = "404", description = "뉴스 기사 정보 없음")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{articleId}")
    public void softDeleteArticle(
            @Parameter(description = "뉴스 기사 ID") @PathVariable UUID articleId) {
        log.info("뉴스 기사 논리 삭제 요청: articleId={}", articleId);
        articleUseCase.softDeleteArticle(articleId);
        log.info("뉴스 기사 논리 삭제 완료: articleId={}", articleId);
    }

    @Operation(summary = "기사 뷰 등록", description = "기사 뷰를 등록합니다.")
    @ApiResponse(responseCode = "200", description = "기사 뷰 등록 성공")
    @ApiResponse(responseCode = "404", description = "뉴스 기사 또는 사용자 정보 없음")
    @RequestId
    @PostMapping("/{articleId}/article-views")
    public ArticleViewDto registerView(
            @Parameter(description = "기사 ID") @PathVariable UUID articleId) {
        return ArticleViewDto.from(articleUseCase.register(articleId));
    }

    @Operation(summary = "뉴스 기사 물리 삭제", description = "뉴스 기사를 물리적으로 삭제합니다.")
    @ApiResponse(responseCode = "204", description = "삭제 성공")
    @ApiResponse(responseCode = "404", description = "뉴스 기사 정보 없음")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{articleId}/hard")
    public void hardDeleteArticle(
            @Parameter(description = "뉴스 기사 ID") @PathVariable UUID articleId) {
        log.info("뉴스 기사 물리 삭제 요청: articleId={}", articleId);
        articleUseCase.hardDeleteArticle(articleId);
        log.info("뉴스 기사 물리 삭제 완료: articleId={}", articleId);
    }
}
