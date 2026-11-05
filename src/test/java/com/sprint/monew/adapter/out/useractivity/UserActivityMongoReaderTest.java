package com.sprint.monew.adapter.out.useractivity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import com.sprint.monew.application.useractivity.provided.dto.UserActivityResult;
import com.sprint.monew.support.MongoIntegrationTest;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@MongoIntegrationTest
class UserActivityMongoReaderTest {

    @Autowired private UserActivityMongoReader reader;
    @Autowired private UserActivityMongoWriter writer;
    @Autowired private UserActivityMongoRepository repository;

    @AfterEach
    void tearDown() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("문서가 있으면 결과 객체로 돌려준다.")
    void givenDocument_whenFind_thenResult() {
        UUID userId = UUID.randomUUID();
        writer.create(userId, "woody@monew.com", "우디", Instant.parse("2026-11-05T00:00:00Z"));

        assertThat(reader.findByUserId(userId))
                .get()
                .extracting(UserActivityResult::nickname)
                .isEqualTo("우디");
    }

    @Test
    @DisplayName("문서가 없으면 빈 값을 돌려준다.")
    void givenNoDocument_whenFind_thenEmpty() {
        assertThat(reader.findByUserId(UUID.randomUUID())).isEmpty();
    }
}
