package com.sprint.monew.adapter.out.interest;

import com.sprint.monew.domain.user.entity.Subscription;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SubscriptionJpaRepository extends JpaRepository<Subscription, UUID> {

    @Query(
            """
            select s.interest.id
            from Subscription s
            where s.user.id = :requestId
            and s.interest.id in :interestIds
            """)
    Set<UUID> findSubscribedInterestIds(UUID requestId, List<UUID> interestIds);
}
