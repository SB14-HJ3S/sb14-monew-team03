package com.sprint.monew.adapter.out.interest;

import com.sprint.monew.domain.interest.entity.Interest;
import java.util.Collection;
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

    int countByIdIn(Collection<UUID> ids);

    @Query(
            "UPDATE Interest i SET i.subscriberCount = i.subscriberCount - 1"
                    + " WHERE i.id in :interestIds AND i.subscriberCount > 0")
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    void decreaseSubscriberCountIn(List<UUID> interestIds);
}
