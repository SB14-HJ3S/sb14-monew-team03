package com.sprint.monew.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.monew.support.MongoIntegrationTest; // 2번에서 본 실제 위치로
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

@MongoIntegrationTest
class MongoConnectionTest {

    private static final String COLLECTION = "mongo_connection_test";

    @Autowired private MongoTemplate mongoTemplate;

    @AfterEach
    void tearDown() {
        mongoTemplate.dropCollection(COLLECTION);
    }

    @Test
    @DisplayName("테스트 프로파일에서 MongoDB에 문서를 저장하고 다시 조회할 수 있다.")
    void givenDocument_whenSaveAndFind_thenSameValues() {
        // given
        UUID userId = UUID.randomUUID();
        Instant now = Instant.parse("2026-11-01T00:00:00Z");
        Document saved =
                new Document("_id", userId)
                        .append("nickname", "우디")
                        .append("createdAt", now)
                        .append("comments", List.of("댓글1", "댓글2"));

        // when
        mongoTemplate.insert(saved, COLLECTION);
        Document found =
                mongoTemplate.findOne(
                        Query.query(Criteria.where("_id").is(userId)), Document.class, COLLECTION);

        // then
        assertThat(found).isNotNull();
        assertThat(found.get("_id", UUID.class)).isEqualTo(userId);
        assertThat(found.getString("nickname")).isEqualTo("우디");
        assertThat(found.getDate("createdAt").toInstant()).isEqualTo(now);
        assertThat(found.getList("comments", String.class)).containsExactly("댓글1", "댓글2");
    }
}
