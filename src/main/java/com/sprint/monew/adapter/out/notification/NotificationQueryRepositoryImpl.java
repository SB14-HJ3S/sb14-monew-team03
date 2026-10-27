package com.sprint.monew.adapter.out.notification;

import static com.sprint.monew.domain.notification.entity.QNotification.notification;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sprint.monew.application.notification.provided.dto.NotificationQuery;
import com.sprint.monew.domain.notification.entity.Notification;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.CommonErrorType;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class NotificationQueryRepositoryImpl implements NotificationQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Notification> findByQuery(UUID userId, NotificationQuery query) {
        // cursor와 after는 둘 다 있거나(다음 페이지) 둘 다 없어야(첫 페이지) 한다
        if (Objects.isNull(query.cursor()) != Objects.isNull(query.after())) {
            throw new MonewException(CommonErrorType.INVALID_REQUEST);
        }

        BooleanExpression where = isUnreadOf(userId);

        if (query.cursor() != null) {
            UUID cursorId = toUuid(query.cursor());
            where =
                    where.and(
                            notification
                                    .createdAt
                                    .lt(query.after())
                                    .or(
                                            notification
                                                    .createdAt
                                                    .eq(query.after())
                                                    .and(notification.id.lt(cursorId))));
        }

        return queryFactory
                .selectFrom(notification)
                .where(where)
                .orderBy(notification.createdAt.desc(), notification.id.desc())
                .limit(query.limit() + 1)
                .fetch();
    }

    @Override
    public long countByQuery(UUID userId, NotificationQuery query) {
        Long count =
                queryFactory
                        .select(notification.count())
                        .from(notification)
                        .where(isUnreadOf(userId))
                        .fetchOne();
        return count == null ? 0 : count;
    }

    private BooleanExpression isUnreadOf(UUID userId) {
        return notification.user.id.eq(userId).and(notification.isRead.isFalse());
    }

    private UUID toUuid(String cursor) {
        try {
            return UUID.fromString(cursor);
        } catch (IllegalArgumentException e) {
            throw new MonewException(CommonErrorType.INVALID_REQUEST, Map.of("cursor", cursor));
        }
    }
}
