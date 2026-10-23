package com.sprint.monew.global.pagination;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
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

    public static <T> PageResult<T> of(
            List<T> content, // 페이지 내용
            String nextCursor, // 다음 페이지 커서
            LocalDateTime nextAfter, // 다음 보조 커서(마지막 요소의 생성 시간)
            int size, // 페이지 크기
            long totalElements, // 총 요소 수
            boolean hasNext) {
        return new PageResult<>(
                content,
                nextCursor,
                Objects.nonNull(nextAfter)
                        ? nextAfter.atZone(ZoneId.systemDefault()).toInstant()
                        : null,
                size,
                totalElements,
                hasNext);
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
