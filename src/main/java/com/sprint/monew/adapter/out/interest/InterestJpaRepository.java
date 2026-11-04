package com.sprint.monew.adapter.out.interest;

import com.sprint.monew.domain.interest.entity.Interest;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InterestJpaRepository
        extends JpaRepository<Interest, UUID>, InterestQueryRepository {

    @Query(
            "UPDATE Interest i SET i.subscriberCount = i.subscriberCount + 1"
                    + " WHERE i.id = :interestId")
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    void increaseSubscriberCount(@Param("interestId") UUID interestId);

    @Query(
            "UPDATE Interest i SET i.subscriberCount = i.subscriberCount - 1"
                    + " WHERE i.id = :interestId AND i.subscriberCount > 0")
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    void decreaseSubscriberCount(@Param("interestId") UUID interestId);

    // 1. 삭제할 userId로 구독 count 감소할 interest를 찾는다.
    // 2. interest를 구독한 user 수만큼 count를 감소시킨다.
    @Query(
            """
            UPDATE Interest i
            SET i.subscriberCount = greatest(
                0L,
                i.subscriberCount - (
                    SELECT COUNT(s)
                    FROM Subscription s
                    WHERE s.interest.id = i.id
                      AND s.user.id IN :userIds
                )
            )
            WHERE i.id IN (
                    SELECT s.interest.id
                    FROM Subscription s
                    WHERE s.user.id IN :userIds
            )
            """)
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    void decreaseSubscriberCountsByUserIds(@Param("userIds") List<UUID> userIds);

    @Query(
            """
            select distinct i
            from Interest i
            join fetch i.keywords
            where i.subscriberCount > 0
            """)
    List<Interest> findSubscribedWithKeywords();
}
