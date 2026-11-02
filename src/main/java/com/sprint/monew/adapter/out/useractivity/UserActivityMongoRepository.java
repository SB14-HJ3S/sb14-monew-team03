package com.sprint.monew.adapter.out.useractivity;

import com.sprint.monew.adapter.out.useractivity.document.UserActivityDocument;
import java.util.UUID;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserActivityMongoRepository extends MongoRepository<UserActivityDocument, UUID> {}
