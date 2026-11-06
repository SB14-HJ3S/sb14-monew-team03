package com.sprint.monew.global.pagination;

import com.sprint.monew.global.entity.BaseEntity;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.CommonErrorType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public record PageResult<T>(
        @Schema(description = "페이지 내용") List<T> content,
        @Schema(description = "다음 페이지 커서(다음 페이지가 없으면 null)") String nextCursor,
        @Schema(description = "다음 보조 커서(마지막 항목의 생성 시각, 다음 페이지가 없으면 null)") Instant nextAfter,
        @Schema(description = "현재 페이지의 항목 수") int size,
        @Schema(description = "검색 조건에 일치하는 전체 항목 수") long totalElements,
        @Schema(description = "다음 페이지 존재 여부") boolean hasNext) {

    public static <T extends BaseEntity> PageResult<T> of(
            List<T> fetched, long totalElements, SortOrder<T> orderBy, int limit) {
        return create(fetched, totalElements, orderBy::extractNextCursorFromLastEntity, limit);
    }

    public static <T extends BaseEntity> PageResult<T> of(
            List<T> fetched, long totalElements, Function<T, String> cursorExtractor, int limit) {
        return create(fetched, totalElements, cursorExtractor, limit);
    }

    private static <T extends BaseEntity> PageResult<T> create(
            List<T> fetched, long totalElements, Function<T, String> cursorExtractor, int limit) {
        if (limit < 1) {
            throw new MonewException(CommonErrorType.INVALID_REQUEST);
        }

        boolean hasNext = fetched.size() > limit;
        List<T> content = List.copyOf(hasNext ? fetched.subList(0, limit) : fetched);

        if (!hasNext) {
            return new PageResult<>(content, null, null, content.size(), totalElements, false);
        }

        T last = content.get(content.size() - 1);
        String nextCursor = cursorExtractor.apply(last);
        Instant nextAfter = last.getCreatedAt();

        if (Objects.isNull(nextCursor) && Objects.isNull(nextAfter)) {
            throw new MonewException(CommonErrorType.INVALID_REQUEST);
        }

        return new PageResult<>(
                content, nextCursor, nextAfter, content.size(), totalElements, true);
    }

    public static <T> PageResult<T> of(
            List<T> fetched,
            long totalElements,
            PageQuery query,
            Function<T, String> cursorExtractor,
            Function<T, Instant> afterExtractor) {
        boolean hasNext = fetched.size() > query.limit();
        List<T> content = List.copyOf(hasNext ? fetched.subList(0, query.limit()) : fetched);
        String nextCursor = null;
        Instant nextAfter = null;

        if (hasNext) {
            T last = content.get(content.size() - 1);
            nextCursor = cursorExtractor.apply(last);
            nextAfter = afterExtractor.apply(last);
        }

        return new PageResult<>(
                content, nextCursor, nextAfter, content.size(), totalElements, hasNext);
    }

    public <R> PageResult<R> map(Function<T, R> mapper) {
        List<R> mappedContent = content.stream().map(mapper).toList();

        return new PageResult<>(mappedContent, nextCursor, nextAfter, size, totalElements, hasNext);
    }
}
