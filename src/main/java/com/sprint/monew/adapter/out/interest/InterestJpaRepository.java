package com.sprint.monew.adapter.out.interest;

import com.sprint.monew.domain.interest.entity.Interest;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterestJpaRepository
        extends JpaRepository<Interest, UUID>, InterestQueryRepository {}
