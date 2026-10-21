package com.sprint.monew.application.interest.provided;

import com.sprint.monew.application.interest.provided.dto.InterestQuery;
import com.sprint.monew.application.interest.provided.dto.InterestResult;
import com.sprint.monew.global.pagination.PageResult;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public interface InterestUseCase {
    PageResult<InterestResult> findByQuery(InterestQuery query);

    InterestResult register(
            @NotBlank @Size(min = 1, max = 50) String name,
            @NotEmpty @Size(min = 1, max = 10) List<@NotBlank String> keywords);

    InterestResult subscribe(UUID interestId, UUID requestId);

    void unsubscribe(UUID interestId, UUID requestId);

    void delete(UUID interestId);

    InterestResult update(UUID interestId, List<String> keywords);
}
