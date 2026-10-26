package com.sprint.monew.adapter.out.interest;

import static com.sprint.monew.domain.interest.entity.QInterest.interest;
import static org.springframework.util.StringUtils.hasText;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sprint.monew.application.interest.provided.dto.InterestQuery;
import com.sprint.monew.domain.interest.entity.Interest;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.CommonErrorType;
import com.sprint.monew.global.exception.errortype.InterestErrorType;
import com.sprint.monew.global.pagination.SortDirection;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
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
        if (Objects.isNull(query.cursor()) != Objects.isNull(query.after())) {
            throw new MonewException(CommonErrorType.INVALID_REQUEST);
        }

        boolean descending = query.direction().equals(SortDirection.DESC);
        InterestAnchor anchor = createAnchor(query.cursor());

        return queryFactory
                .selectFrom(interest)
                .where(containsKeyword(query.keyword()))
                .where(
                        matchPaginationCondition(
                                query.cursor(), query.after(), query.orderBy(), descending, anchor))
                .orderBy(
                        cursorOrder(query.orderBy(), descending),
                        createdAtOrder(descending),
                        idOrder(descending))
                .limit(query.limit() + 1)
                .fetch();
    }

    private InterestAnchor createAnchor(String cursor) {
        if (Objects.isNull(cursor)) {
            return null;
        }

        UUID cursorId;
        try {
            cursorId = UUID.fromString(cursor);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new MonewException(CommonErrorType.INVALID_REQUEST, e);
        }

        Tuple row =
                Optional.ofNullable(
                                queryFactory
                                        .select(interest.name, interest.subscriberCount)
                                        .from(interest)
                                        .where(interest.id.eq(cursorId))
                                        .fetchOne())
                        .orElseThrow(
                                () -> new MonewException(InterestErrorType.INTEREST_NOT_FOUND));

        return new InterestAnchor(
                cursorId, row.get(interest.name), row.get(interest.subscriberCount));
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
            String cursor,
            Instant createdAt,
            InterestQuery.SortOrder sortOrder,
            boolean descending,
            InterestAnchor anchor) {
        if (Objects.isNull(cursor) && Objects.isNull(createdAt)) {
            return null;
        }

        BooleanExpression createdAtCondition =
                descending ? interest.createdAt.lt(createdAt) : interest.createdAt.gt(createdAt);
        BooleanExpression createdAtEqual = interest.createdAt.eq(createdAt);

        BooleanExpression idCondition =
                descending ? interest.id.lt(anchor.id()) : interest.id.gt(anchor.id());

        return switch (sortOrder) {
            case NAME -> {
                BooleanExpression nameEqual = interest.name.eq(anchor.name());
                BooleanExpression nameCondition =
                        descending
                                ? interest.name.lt(anchor.name())
                                : interest.name.gt(anchor.name());

                yield nameCondition
                        .or(nameEqual.and(createdAtCondition))
                        .or(nameEqual.and(createdAtEqual).and(idCondition));
            }
            case SUBSCRIBER_COUNT -> {
                BooleanExpression subscriberCountEqual =
                        interest.subscriberCount.eq(anchor.subscriberCount());
                BooleanExpression subscriberCountCondition =
                        descending
                                ? interest.subscriberCount.lt(anchor.subscriberCount())
                                : interest.subscriberCount.gt(anchor.subscriberCount());

                yield subscriberCountCondition
                        .or(subscriberCountEqual.and(createdAtCondition))
                        .or(subscriberCountEqual.and(createdAtEqual).and(idCondition));
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
