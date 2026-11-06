package com.sprint.monew.adapter.out.useractivity;

import com.sprint.monew.adapter.out.useractivity.document.UserActivityDocument;
import com.sprint.monew.application.useractivity.provided.dto.UserActivityResult;
import com.sprint.monew.application.useractivity.required.UserActivityReader;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserActivityMongoReader implements UserActivityReader {

    private final UserActivityMongoRepository repository;

    @Override
    public Optional<UserActivityResult> findByUserId(UUID userId) {
        return repository.findById(userId).map(UserActivityDocument::toResult);
    }
}
