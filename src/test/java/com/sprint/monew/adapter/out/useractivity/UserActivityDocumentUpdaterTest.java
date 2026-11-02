package com.sprint.monew.adapter.out.useractivity;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.monew.adapter.out.useractivity.document.CommentItem;
import com.sprint.monew.adapter.out.useractivity.document.UserActivityDocument;
import com.sprint.monew.support.MongoIntegrationTest;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@MongoIntegrationTest
class UserActivityDocumentUpdaterTest {

    private static final Instant T = Instant.parse("2026-11-02T00:00:00Z");

    @Autowired private UserActivityMongoRepository repository;
    @Autowired private UserActivityDocumentUpdater updater;

    @AfterEach
    void tearDown() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("댓글을 11번 추가하면 최신 10건만 최신순으로 남는다.")
    void givenElevenComments_whenPush_thenKeepsLatestTen() {
        // given
        UUID userId = UUID.randomUUID();
        repository.save(UserActivityDocument.create(userId, "woody@monew.com", "우디", T));

        // when
        for (int i = 1; i <= 11; i++) {
            updater.pushComment(
                    userId,
                    new CommentItem(
                            UUID.randomUUID(),
                            UUID.randomUUID(),
                            "기사",
                            userId,
                            "우디",
                            "댓글" + i,
                            0L,
                            T.plusSeconds(i)));
        }

        // then
        UserActivityDocument found = repository.findById(userId).orElseThrow();
        assertThat(found.comments()).hasSize(UserActivityDocument.RECENT_LIMIT);
        assertThat(found.comments().get(0).content()).isEqualTo("댓글11"); // 최신이 맨 앞
        assertThat(found.comments())
                .extracting(CommentItem::content)
                .doesNotContain("댓글1"); // 가장 오래된 것 밀려남
    }
}
