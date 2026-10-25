package com.sprint.monew.global.pagination;

import com.sprint.monew.global.entity.BaseEntity;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.CommonErrorType;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public record PageResult<T>(
        List<T> content, // 페이지 내용
        String nextCursor, // 다음 페이지 커서
        Instant nextAfter, // 다음 보조 커서(마지막 요소의 생성 시간)
        int size, // 페이지 크기
        long totalElements, // 총 요소 수
        boolean hasNext) {

    public static <T extends BaseEntity, S extends SortOrder<T>> PageResult<T> of(
            List<T> fetched, long totalElements, S orderBy, int limit) {
        return create(fetched, totalElements, orderBy::cursorToString, limit);
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

    public <R> PageResult<R> map(Function<T, R> mapper) {
        List<R> mappedContent = content.stream().map(mapper).toList();

        return new PageResult<>(mappedContent, nextCursor, nextAfter, size, totalElements, hasNext);
    }
}
