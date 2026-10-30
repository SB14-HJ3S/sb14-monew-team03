package com.sprint.monew.adapter.out.useractivity;

import static com.sprint.monew.domain.user.entity.QUser.user;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sprint.monew.application.notification.required.UserActivityReader;
import com.sprint.monew.application.useractivity.provided.dto.UserActivityResult;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserActivityRdbAdapter implements UserActivityReader {

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
                        List.of(), // TODO: 구독 관심사
                        List.of(), // TODO: 최근 댓글
                        List.of(), // TODO: 최근 좋아요
                        List.of())); // TODO: 최근 본 기사
    }
}
