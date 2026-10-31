package com.sprint.monew.adapter.out.useractivity;

import static com.sprint.monew.domain.article.entity.QArticle.article;
import static com.sprint.monew.domain.article.entity.QArticleViewStatus.articleViewStatus;
import static com.sprint.monew.domain.comment.entity.QComment.comment;
import static com.sprint.monew.domain.comment.entity.QCommentLike.commentLike;
import static com.sprint.monew.domain.interest.entity.QInterest.interest;
import static com.sprint.monew.domain.interest.entity.QInterestKeyword.interestKeyword;
import static com.sprint.monew.domain.user.entity.QSubscription.subscription;
import static com.sprint.monew.domain.user.entity.QUser.user;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sprint.monew.application.notification.required.UserActivityReader;
import com.sprint.monew.application.useractivity.provided.dto.ArticleViewActivityResult;
import com.sprint.monew.application.useractivity.provided.dto.CommentActivityResult;
import com.sprint.monew.application.useractivity.provided.dto.CommentLikeActivityResult;
import com.sprint.monew.application.useractivity.provided.dto.SubscriptionActivityResult;
import com.sprint.monew.application.useractivity.provided.dto.UserActivityResult;
import com.sprint.monew.domain.user.entity.QUser;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserActivityRdbAdapter implements UserActivityReader {

    private static final int RECENT_LIMIT = 10;
    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<UserActivityResult> findByUserId(UUID userId) {
        Tuple found =
                queryFactory
                        .select(user.id, user.email, user.nickname, user.createdAt)
                        .from(user)
                        .where(user.id.eq(userId), user.deletedAt.isNull())
                        .fetchOne();

        if (found == null) {
            return Optional.empty();
        }

        return Optional.of(
                new UserActivityResult(
                        found.get(user.id),
                        found.get(user.email),
                        found.get(user.nickname),
                        found.get(user.createdAt),
                        findSubscriptions(userId),
                        findRecentComments(userId),
                        findRecentCommentLikes(userId),
                        findRecentArticleViews(userId)));
    }

    private List<SubscriptionActivityResult> findSubscriptions(UUID userId) {
        List<Tuple> rows =
                queryFactory
                        .select(
                                subscription.id,
                                subscription.createdAt,
                                interest.id,
                                interest.name,
                                interest.subscriberCount)
                        .from(subscription)
                        .join(subscription.interest, interest)
                        .where(subscription.user.id.eq(userId))
                        .orderBy(subscription.createdAt.desc())
                        .fetch();

        if (rows.isEmpty()) {
            return List.of();
        }

        // 키워드는 별도 테이블이라 관심사 ID로 한 번에 조회해서 붙인다
        List<UUID> interestIds = rows.stream().map(r -> r.get(interest.id)).toList();
        Map<UUID, List<String>> keywordsByInterest = findKeywords(interestIds);

        return rows.stream()
                .map(
                        r ->
                                new SubscriptionActivityResult(
                                        r.get(subscription.id),
                                        r.get(interest.id),
                                        r.get(interest.name),
                                        keywordsByInterest.getOrDefault(
                                                r.get(interest.id), List.of()),
                                        r.get(interest.subscriberCount),
                                        r.get(subscription.createdAt)))
                .toList();
    }

    private Map<UUID, List<String>> findKeywords(List<UUID> interestIds) {
        return queryFactory
                .select(interestKeyword.interest.id, interestKeyword.keyword)
                .from(interestKeyword)
                .where(interestKeyword.interest.id.in(interestIds))
                .orderBy(interestKeyword.createdAt.asc()) // 등록한 순서대로
                .fetch()
                .stream()
                .collect(
                        Collectors.groupingBy(
                                r -> r.get(interestKeyword.interest.id),
                                Collectors.mapping(
                                        r -> r.get(interestKeyword.keyword), Collectors.toList())));
    }

    // ===== 최근 작성한 댓글 (최대 10건) =====

    private List<CommentActivityResult> findRecentComments(UUID userId) {
        return queryFactory
                .select(
                        Projections.constructor(
                                CommentActivityResult.class,
                                comment.id,
                                article.id,
                                article.title,
                                user.id,
                                user.nickname,
                                comment.content,
                                comment.likeCount,
                                comment.createdAt))
                .from(comment)
                .join(comment.article, article)
                .join(comment.user, user)
                .where(user.id.eq(userId), comment.deletedAt.isNull(), article.deletedAt.isNull())
                .orderBy(comment.createdAt.desc())
                .limit(RECENT_LIMIT)
                .fetch();
    }

    // ===== 최근 좋아요 누른 댓글 (최대 10건) =====

    private List<CommentLikeActivityResult> findRecentCommentLikes(UUID userId) {
        QUser commentUser = new QUser("commentUser"); // 댓글 작성자 (좋아요 누른 사람과 구분)

        return queryFactory
                .select(
                        Projections.constructor(
                                CommentLikeActivityResult.class,
                                commentLike.id,
                                commentLike.createdAt,
                                comment.id,
                                article.id,
                                article.title,
                                commentUser.id,
                                commentUser.nickname,
                                comment.content,
                                comment.likeCount,
                                comment.createdAt))
                .from(commentLike)
                .join(commentLike.comment, comment)
                .join(comment.article, article)
                .join(comment.user, commentUser)
                .where(
                        commentLike.user.id.eq(userId), // 좋아요 누른 사람 = 나
                        comment.deletedAt.isNull(),
                        article.deletedAt.isNull())
                .orderBy(commentLike.createdAt.desc())
                .limit(RECENT_LIMIT)
                .fetch();
    }

    // ===== 최근 본 기사 (최대 10건, 마지막으로 본 순) =====

    private List<ArticleViewActivityResult> findRecentArticleViews(UUID userId) {
        return queryFactory
                .select(
                        articleViewStatus.id,
                        articleViewStatus.lastViewedAt,
                        article.id,
                        article.source,
                        article.sourceUrl,
                        article.title,
                        article.publishedAt,
                        article.summary,
                        article.commentCount,
                        article.viewCount)
                .from(articleViewStatus)
                .join(articleViewStatus.article, article)
                .where(articleViewStatus.user.id.eq(userId), article.deletedAt.isNull())
                .orderBy(articleViewStatus.lastViewedAt.desc()) // 다시 본 기사가 위로
                .limit(RECENT_LIMIT)
                .fetch()
                .stream()
                .map(
                        r ->
                                new ArticleViewActivityResult(
                                        r.get(articleViewStatus.id),
                                        userId, // viewedBy = 조회 조건의 사용자
                                        toInstant(r.get(articleViewStatus.lastViewedAt)), // 본 시각
                                        r.get(article.id),
                                        r.get(article.source), // enum → "NAVER"
                                        r.get(article.sourceUrl),
                                        r.get(article.title),
                                        toInstant(r.get(article.publishedAt)),
                                        r.get(article.summary),
                                        r.get(article.commentCount), // int → long 자동 변환
                                        r.get(article.viewCount)))
                .toList();
    }

    // LocalDateTime(시간대 없음)을 Instant로. 서버 기본 시간대 기준
    private Instant toInstant(LocalDateTime time) {
        return time.atZone(ZoneId.systemDefault()).toInstant();
    }
}
