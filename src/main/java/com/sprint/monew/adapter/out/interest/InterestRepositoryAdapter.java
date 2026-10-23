package com.sprint.monew.adapter.out.interest;

import com.sprint.monew.application.interest.provided.dto.InterestQuery;
import com.sprint.monew.application.interest.required.InterestRepository;
import com.sprint.monew.domain.interest.entity.Interest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class InterestRepositoryAdapter implements InterestRepository {
    private final InterestJpaRepository jpaRepository;

    @Override
    public Interest save(Interest entity) {
        return jpaRepository.save(entity);
    }

    @Override
    public List<String> findAllNames() {
        return jpaRepository.findAllNames();
    }

    @Override
    public List<Interest> findByQuery(InterestQuery query) {
        return jpaRepository.findByQuery(query);
    }

    @Override
    public long countByQuery(InterestQuery query) {
        return jpaRepository.countByQuery(query);
    }
}
