package com.sprint.monew.adapter.out.article;

import static com.sprint.monew.domain.article.entity.QArticle.article;
import static com.sprint.monew.domain.article.entity.QArticleInterest.articleInterest;
import static org.springframework.util.StringUtils.hasText;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sprint.monew.application.article.provided.dto.ArticleQuery;
import com.sprint.monew.domain.article.entity.Article;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.CommonErrorType;
import com.sprint.monew.global.pagination.SortDirection;
import com.sprint.monew.global.type.ArticleSourceType;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ArticleQueryRepositoryImpl implements ArticleQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Article> findByQuery(ArticleQuery query) {
        boolean descending = query.direction() == SortDirection.DESC;

        return queryFactory
                .selectFrom(article)
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
    public long countByQuery(ArticleQuery query) {
        Long count =
                queryFactory
                        .select(article.count())
                        .from(article)
                        .where(searchConditions(query))
                        .fetchOne();

        return Objects.requireNonNullElse(count, 0L);
    }

    // ==== where 절: FILTER 관련 ===================================================

    private BooleanExpression[] searchConditions(ArticleQuery condition) {
        return new BooleanExpression[] {
            article.deletedAt.isNull(),
            titleOrSummaryContains(condition.keyword()),
            interestEquals(condition.interestId()),
            sourceIn(condition.sourceIn()),
            publishDateGreaterThanOrEqual(condition.publishDateFrom()),
            publishDateLessThanOrEqual(condition.publishDateTo())
        };
    }

    private BooleanExpression titleOrSummaryContains(String keyword) {
        return hasText(keyword)
                ? article.title
                        .containsIgnoreCase(keyword)
                        .or(article.summary.containsIgnoreCase(keyword))
                : null;
    }

    private BooleanExpression interestEquals(UUID interestId) { // ArticleInterest 중간테이블 조회
        if (Objects.isNull(interestId)) return null;

        BooleanExpression currentArticle = articleInterest.article.id.eq(article.id);
        BooleanExpression targetInterest = articleInterest.interest.id.eq(interestId);

        // 서브쿼리
        return JPAExpressions.selectOne()
                .from(articleInterest)
                .where(currentArticle, targetInterest)
                .exists();
    }

    private BooleanExpression sourceIn(List<ArticleSourceType> sources) {
        if (Objects.isNull(sources) || sources.isEmpty()) {
            return null;
        }

        return article.source.in(sources); // 네이버/한경/조선/연합 중 하나인지
    }

    private BooleanExpression publishDateGreaterThanOrEqual(LocalDateTime from) {
        return Objects.nonNull(from) ? article.publishedAt.goe(from) : null;
    }

    private BooleanExpression publishDateLessThanOrEqual(LocalDateTime to) {
        return Objects.nonNull(to) ? article.publishedAt.loe(to) : null;
    }

    // ==== where 절: Cursor 관련 =======================================================

    public BooleanExpression cursorCondition(
            String cursor, Instant after, ArticleQuery.SortOrder sortField, boolean descending) {
        if (!hasText(cursor) && Objects.isNull(after)) return null;
        if (!hasText(cursor) || Objects.isNull(after)) {
            throw new MonewException(
                    CommonErrorType.PAGINATION_NOT_BOTH_CURSOR_AND_AFTER,
                    Map.of(
                            "cursor", Objects.toString(cursor, "null"),
                            "after", Objects.toString(after, "null")));
        }

        BooleanExpression createdAtCondition =
                descending ? article.createdAt.lt(after) : article.createdAt.gt(after);

        return switch (sortField) {
            case PUBLISH_DATE -> {
                LocalDateTime value = LocalDateTime.parse(cursor);

                yield descending
                        ? article.publishedAt
                                .lt(value)
                                .or(article.publishedAt.eq(value).and(createdAtCondition))
                        : article.publishedAt
                                .gt(value)
                                .or(article.publishedAt.eq(value).and(createdAtCondition));
            }
            case COMMENT_COUNT -> {
                int value = Integer.parseInt(cursor);

                yield descending
                        ? article.commentCount
                                .lt(value)
                                .or(article.commentCount.eq(value).and(createdAtCondition))
                        : article.commentCount
                                .gt(value)
                                .or(article.commentCount.eq(value).and(createdAtCondition));
            }
            case VIEW_COUNT -> {
                int value = Integer.parseInt(cursor);

                yield descending
                        ? article.viewCount
                                .lt(value)
                                .or(article.viewCount.eq(value).and(createdAtCondition))
                        : article.viewCount
                                .gt(value)
                                .or(article.viewCount.eq(value).and(createdAtCondition));
            }
        };
    }

    private OrderSpecifier<?> primarySort(ArticleQuery.SortOrder sortField, boolean descending) {
        return switch (sortField) {
            case PUBLISH_DATE ->
                    descending ? article.publishedAt.desc() : article.publishedAt.asc();
            case COMMENT_COUNT ->
                    descending ? article.commentCount.desc() : article.commentCount.asc();
            case VIEW_COUNT -> descending ? article.viewCount.desc() : article.viewCount.asc();
        };
    }

    private OrderSpecifier<?> secondarySort(boolean descending) {
        return descending ? article.createdAt.desc() : article.createdAt.asc();
    }

    private OrderSpecifier<?> finalSort(boolean descending) {
        return descending ? article.id.desc() : article.id.asc();
    }
}
