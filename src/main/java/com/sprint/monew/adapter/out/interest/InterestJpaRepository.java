package com.sprint.monew.adapter.out.interest;

import com.sprint.monew.domain.interest.entity.Interest;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface InterestJpaRepository
        extends JpaRepository<Interest, UUID>, InterestQueryRepository {

    @Query(
            """
        select distinct i
        from Interest i
        join fetch i.keywords
        """)
    List<Interest> findSubscribedWithKeywords();
}
