package com.sprint.monew.adapter.out.repository.comment;

import static com.sprint.monew.domain.comment.entity.QComment.comment;
import static com.sprint.monew.domain.user.entity.QUser.user;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sprint.monew.application.comment.provided.usecase.dto.CommentQuery;
import com.sprint.monew.domain.comment.entity.Comment;
import com.sprint.monew.global.pagination.SortDirection;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CommentQueryRepositoryImpl implements CommentQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Comment> findByQuery(CommentQuery query, Comment cursorComment) {
        boolean descending = query.direction() == SortDirection.DESC;

        // 응답에 작성자 닉네임이 필요해서 작성자도 같이 가져옴 (댓글마다 따로 조회 안 하게)
        return queryFactory
                .selectFrom(comment)
                .join(comment.user, user)
                .fetchJoin()
                .where(searchConditions(query))
                .where(cursorCondition(cursorComment, query.orderBy(), descending))
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

    // 정렬 3단계(기준 → 작성 시각 → id)와 같은 순서로 이어서 빠지는 댓글 없게 함
    private BooleanExpression cursorCondition(
            Comment cursor, CommentQuery.SortOrder sortField, boolean descending) {
        if (Objects.isNull(cursor)) {
            return null;
        }

        Instant createdAt = cursor.getCreatedAt();
        // 작성 시각이 같으면 id로 이어감
        BooleanExpression afterCursor =
                (descending ? comment.createdAt.lt(createdAt) : comment.createdAt.gt(createdAt))
                        .or(
                                comment.createdAt
                                        .eq(createdAt)
                                        .and(
                                                descending
                                                        ? comment.id.lt(cursor.getId())
                                                        : comment.id.gt(cursor.getId())));

        return switch (sortField) {
            case CREATED_AT -> afterCursor;
            case LIKE_COUNT -> {
                long likeCount = cursor.getLikeCount();

                // 좋아요 수가 같으면 작성 시각·id로 이어감
                yield (descending
                                ? comment.likeCount.lt(likeCount)
                                : comment.likeCount.gt(likeCount))
                        .or(comment.likeCount.eq(likeCount).and(afterCursor));
            }
        };
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
