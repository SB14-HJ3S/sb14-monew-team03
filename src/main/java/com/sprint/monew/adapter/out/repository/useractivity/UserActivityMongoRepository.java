package com.sprint.monew.adapter.out.repository.useractivity;

import com.sprint.monew.adapter.out.repository.useractivity.document.UserActivityDocument;
import java.util.UUID;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserActivityMongoRepository extends MongoRepository<UserActivityDocument, UUID> {}
