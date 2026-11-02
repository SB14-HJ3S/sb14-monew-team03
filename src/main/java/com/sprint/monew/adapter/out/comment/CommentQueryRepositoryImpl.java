package com.sprint.monew.adapter.out.comment;

import static com.sprint.monew.domain.comment.entity.QComment.comment;
import static com.sprint.monew.domain.user.entity.QUser.user;
import static org.springframework.util.StringUtils.hasText;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sprint.monew.application.comment.provided.dto.CommentQuery;
import com.sprint.monew.domain.comment.entity.Comment;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.CommonErrorType;
import com.sprint.monew.global.pagination.SortDirection;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CommentQueryRepositoryImpl implements CommentQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Comment> findByQuery(CommentQuery query) {
        boolean descending = query.direction() == SortDirection.DESC;

        // 응답에 작성자 닉네임이 필요해서 작성자도 같이 가져옴 (댓글마다 따로 조회 안 하게)
        return queryFactory
                .selectFrom(comment)
                .join(comment.user, user)
                .fetchJoin()
                .where(searchConditions(query))
                .where(cursorCondition(query.cursor(), query.after(), query.orderBy(), descending))
                .orderBy(
                        primarySort(query.orderBy(), descending),
                        secondarySort(descending),
                        finalSort(descending))
                .limit((long) query.limit() + 1)
                .fetch();
    }

    @Override
    public long countByQuery(CommentQuery query) {
        Long count =
                queryFactory
                        .select(comment.count())
                        .from(comment)
                        .where(searchConditions(query))
                        .fetchOne();

        return Objects.requireNonNullElse(count, 0L);
    }

    private BooleanExpression[] searchConditions(CommentQuery query) {
        return new BooleanExpression[] {
            comment.deletedAt.isNull(), articleEquals(query.articleId())
        };
    }

    private BooleanExpression articleEquals(UUID articleId) {
        return Objects.nonNull(articleId) ? comment.article.id.eq(articleId) : null;
    }

    // 커서는 정렬 기준 값, after는 마지막 댓글의 작성 시각
    private BooleanExpression cursorCondition(
            String cursor, Instant after, CommentQuery.SortOrder sortField, boolean descending) {
        if (!hasText(cursor) && Objects.isNull(after)) {
            return null;
        }

        if (!hasText(cursor) || Objects.isNull(after)) {
            throw new MonewException(
                    CommonErrorType.PAGINATION_NOT_BOTH_CURSOR_AND_AFTER,
                    Map.of(
                            "cursor", Objects.toString(cursor, "null"),
                            "after", Objects.toString(after, "null")));
        }

        BooleanExpression createdAtCondition =
                descending ? comment.createdAt.lt(after) : comment.createdAt.gt(after);

        try {
            return switch (sortField) {
                case CREATED_AT -> {
                    Instant value = Instant.parse(cursor);

                    yield descending ? comment.createdAt.lt(value) : comment.createdAt.gt(value);
                }
                case LIKE_COUNT -> {
                    long value = Long.parseLong(cursor);

                    // 좋아요 수가 같은 댓글은 작성 시각으로 이어서 빠지는 댓글 없게 함
                    yield descending
                            ? comment.likeCount
                                    .lt(value)
                                    .or(comment.likeCount.eq(value).and(createdAtCondition))
                            : comment.likeCount
                                    .gt(value)
                                    .or(comment.likeCount.eq(value).and(createdAtCondition));
                }
            };
        } catch (DateTimeParseException | NumberFormatException e) {
            throw new MonewException(CommonErrorType.INVALID_REQUEST, Map.of("cursor", cursor), e);
        }
    }

    private OrderSpecifier<?> primarySort(CommentQuery.SortOrder sortField, boolean descending) {
        return switch (sortField) {
            case CREATED_AT -> descending ? comment.createdAt.desc() : comment.createdAt.asc();
            case LIKE_COUNT -> descending ? comment.likeCount.desc() : comment.likeCount.asc();
        };
    }

    private OrderSpecifier<?> secondarySort(boolean descending) {
        return descending ? comment.createdAt.desc() : comment.createdAt.asc();
    }

    private OrderSpecifier<?> finalSort(boolean descending) {
        return descending ? comment.id.desc() : comment.id.asc();
    }
}
