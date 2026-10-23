package com.sprint.monew.adapter.out.interest;

import static com.sprint.monew.domain.interest.entity.QInterest.interest;
import static org.springframework.util.StringUtils.hasText;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sprint.monew.application.interest.provided.dto.InterestQuery;
import com.sprint.monew.domain.interest.entity.Interest;
import com.sprint.monew.global.pagination.SortDirection;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class InterestQueryRepositoryImpl implements InterestQueryRepository {
    private final JPAQueryFactory queryFactory;

    @Override
    public List<String> findAllNames() {
        return queryFactory.select(interest.name).from(interest).fetch();
    }

    @Override
    public List<Interest> findByQuery(InterestQuery query) {
        boolean descending = query.direction().equals(SortDirection.DESC);
        return queryFactory
                .selectFrom(interest)
                .where(containsKeyword(query.keyword()))
                .where(
                        matchPaginationCondition(
                                query.cursor(), query.after(), query.orderBy(), descending))
                .orderBy(
                        cursorOrder(query.orderBy(), descending),
                        createdAtOrder(descending),
                        idOrder(descending))
                .limit(query.limit() + 1)
                .fetch();
    }

    @Override
    public long countByQuery(InterestQuery query) {
        Long count =
                queryFactory
                        .select(interest.count())
                        .from(interest)
                        .where(containsKeyword(query.keyword()))
                        .fetchOne();

        return Optional.ofNullable(count).orElse(0L);
    }

    private BooleanExpression containsKeyword(String keyword) {
        return hasText(keyword)
                ? interest.keywords
                        .any()
                        .keyword
                        .containsIgnoreCase(keyword)
                        .or(interest.name.containsIgnoreCase(keyword))
                : null;
    }

    private BooleanExpression matchPaginationCondition(
            String cursor, Instant after, InterestQuery.SortOrder sortOrder, boolean descending) {
        if (Objects.isNull(cursor) || Objects.isNull(after)) {
            return null;
        }

        BooleanExpression createdAtCondition =
                descending ? interest.createdAt.lt(after) : interest.createdAt.gt(after);

        return switch (sortOrder) {
            case NAME -> {
                String name = cursor;
                yield descending ? interest.name.lt(name) : interest.name.gt(name);
            }
            case SUBSCRIBER_COUNT -> {
                Long subscriberCount = Long.valueOf(cursor);
                yield descending
                        ? interest.subscriberCount
                                .lt(subscriberCount)
                                .or(
                                        interest.subscriberCount
                                                .eq(subscriberCount)
                                                .and(createdAtCondition))
                        : interest.subscriberCount
                                .gt(subscriberCount)
                                .or(
                                        interest.subscriberCount
                                                .eq(subscriberCount)
                                                .and(createdAtCondition));
            }
        };
    }

    private OrderSpecifier<?> cursorOrder(InterestQuery.SortOrder sortOrder, boolean descending) {
        return switch (sortOrder) {
            case NAME -> descending ? interest.name.desc() : interest.name.asc();
            case SUBSCRIBER_COUNT ->
                    descending ? interest.subscriberCount.desc() : interest.subscriberCount.asc();
        };
    }

    private OrderSpecifier<?> createdAtOrder(boolean descending) {
        return descending ? interest.createdAt.desc() : interest.createdAt.asc();
    }

    private OrderSpecifier<?> idOrder(boolean descending) {
        return descending ? interest.id.desc() : interest.id.asc();
    }
}
