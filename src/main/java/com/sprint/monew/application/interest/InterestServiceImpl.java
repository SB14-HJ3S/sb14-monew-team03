package com.sprint.monew.application.interest;

import com.sprint.monew.application.interest.provided.dto.InterestQuery;
import com.sprint.monew.application.interest.required.DuplicateInterestNameException;
import com.sprint.monew.application.interest.required.InterestRepository;
import com.sprint.monew.domain.interest.entity.Interest;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.InterestErrorType;
import com.sprint.monew.global.pagination.PageResult;
import com.sprint.monew.global.util.similarity.StringSimilarityUtils;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InterestServiceImpl implements InterestService {

    private static final double NAME_SIMILARITY_THRESHOLD = 0.8D;

    private final InterestRepository repository;

    @Override
    public Interest create(String name, List<String> keywords) {
        // name이 80% 이상 유사한 이름의 관심사가 있다면 등록할 수 없습니다.
        try {
            validateSimilarNameDoesNotExist(name);
            Interest interest = Interest.create(name, keywords);
            return repository.save(interest);
        } catch (DuplicateInterestNameException e) {
            throw new MonewException(
                    InterestErrorType.SIMILAR_NAME_CONFLICT, Map.of("name", name), e);
        }
    }

    private void validateSimilarNameDoesNotExist(String name) {
        repository.findAllNames().stream()
                .filter(existing -> isSimilarityMoreThanThreshold(name, existing))
                .findAny()
                .ifPresent(
                        similar -> {
                            throw new MonewException(
                                    InterestErrorType.SIMILAR_NAME_CONFLICT,
                                    Map.of(
                                            "requestedName",
                                            name,
                                            "existingName",
                                            similar,
                                            "similarity",
                                            StringSimilarityUtils.calculateSimilarity(
                                                    name, similar)));
                        });
    }

    private boolean isSimilarityMoreThanThreshold(String requestingName, String existingName) {
        return StringSimilarityUtils.calculateSimilarity(requestingName, existingName)
                >= NAME_SIMILARITY_THRESHOLD;
    }

    @Override
    public PageResult<Interest> findPageByQuery(InterestQuery query) {
        return PageResult.of(
                repository.findByQuery(query),
                repository.countByQuery(query),
                query.orderBy(),
                query.limit());
    }

    @Override
    public Interest update(UUID interestId, List<String> keywords) {
        Interest updating =
                repository
                        .findByIdWithKeywords(interestId)
                        .orElseThrow(
                                () -> new MonewException(InterestErrorType.INTEREST_NOT_FOUND));
        return updating.updateKeywords(keywords);
    }

    @Override
    public Interest getById(UUID interestId) {
        return repository
                .findById(interestId)
                .orElseThrow(
                        () ->
                                new MonewException(
                                        InterestErrorType.INTEREST_NOT_FOUND,
                                        Map.of("id", interestId)));
    }

    @Override
    public void delete(Interest deleting) {
        repository.delete(deleting);
    }

    @Override
    public Interest increaseSubscriberCount(UUID interestId) {
        repository.increaseSubscriberCount(interestId);
        return getById(interestId);
    }

    @Override
    public void decreaseSubscriberCount(UUID interestId) {
        validateAlreadyExistsById(interestId);
        repository.decreaseSubscriberCount(interestId);
    }

    @Override
    public void decreaseSubscriberCount(List<UUID> interestIds) {
        validateExistsByIds(interestIds);
        repository.decreaseSubscriberCountIn(interestIds);
    }

    private boolean existsById(UUID interestId) {
        return repository.existsById(interestId);
    }

    private void validateAlreadyExistsById(UUID interestId) {
        if (!existsById(interestId)) {
            throw new MonewException(
                    InterestErrorType.INTEREST_NOT_FOUND, Map.of("id", interestId));
        }
    }

    private void validateExistsByIds(List<UUID> interestId) {
        if (repository.countByIdIn(interestId) != interestId.size()) {
            throw new MonewException(
                    InterestErrorType.INTEREST_NOT_FOUND, Map.of("id", interestId));
        }
    }
}
