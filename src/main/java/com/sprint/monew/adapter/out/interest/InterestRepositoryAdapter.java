package com.sprint.monew.adapter.out.interest;

import com.sprint.monew.application.interest.provided.dto.InterestQuery;
import com.sprint.monew.application.interest.required.DuplicateInterestNameException;
import com.sprint.monew.application.interest.required.InterestRepository;
import com.sprint.monew.domain.interest.entity.Interest;
import com.sprint.monew.global.util.DatabaseConstraintUtils;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class InterestRepositoryAdapter implements InterestRepository {

    private final InterestJpaRepository jpaRepository;

    @Override
    public Interest save(Interest entity) {
        try {
            return jpaRepository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException e) {
            if (DatabaseConstraintUtils.isUniqueConstraintViolation(e)) {
                throw new DuplicateInterestNameException(e, entity.getId(), entity.getName());
            }
            throw e;
        }
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

    @Override
    public Optional<Interest> findByIdWithKeywords(UUID interestId) {
        return Optional.ofNullable(jpaRepository.findByIdWithKeywords(interestId));
    }

    @Override
    public void delete(Interest entity) {
        jpaRepository.delete(entity);
    }

    @Override
    public Optional<Interest> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public void increaseSubscriberCount(UUID interestId) {
        jpaRepository.increaseSubscriberCount(interestId);
    }

    @Override
    public void decreaseSubscriberCount(UUID interestId) {
        jpaRepository.decreaseSubscriberCount(interestId);
    }

    @Override
    public boolean existsById(UUID interestId) {
        return jpaRepository.existsById(interestId);
    }

    @Override
    public int countByIdIn(List<UUID> interestId) {
        return jpaRepository.countByIdIn((interestId));
    }

    @Override
    public void decreaseSubscriberCountIn(List<UUID> interestIds) {
        jpaRepository.decreaseSubscriberCountIn(interestIds);
    }

    @Override
    public List<Interest> findSubscribedWithKeywords() {
        return jpaRepository.findSubscribedWithKeywords();
    }
}
