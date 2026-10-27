package com.sprint.monew.adapter.out.notification;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import com.sprint.monew.domain.notification.entity.Notification;
import com.sprint.monew.domain.user.entity.User;
import com.sprint.monew.global.type.ResourceType;
import com.sprint.monew.support.IntegrationTest;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@IntegrationTest
@Transactional
class NotificationRepositoryTest {

    private static final Instant T = Instant.parse("2026-10-27T01:05:00Z");

    @Autowired NotificationJpaRepository notificationJpaRepository;
    @Autowired EntityManager em;
    @Autowired JdbcTemplate jdbcTemplate;

    User woody;
    User buzz;

    @BeforeEach
    void setUp() {
        woody = saveUser("woody@monew.com", "우디");
        buzz = saveUser("buzz@monew.com", "버즈");
    }

    @Test
    void 전체_확인은_내_미확인_알림만_읽음으로_바꾼다() {

        // given
        UUID unread1 = saveNotification(woody, T, false);
        UUID unread2 = saveNotification(woody, T.minusSeconds(60), false);
        UUID alreadyRead = saveNotification(woody, T.minusSeconds(120), true);
        UUID buzzUnread = saveNotification(buzz, T, false);
        clearContext();

        LocalDateTime alreadyReadAt =
                notificationJpaRepository.findById(alreadyRead).orElseThrow().getReadAt();
        LocalDateTime now = LocalDateTime.of(2026, 10, 27, 12, 0);

        // when
        int updated = notificationJpaRepository.markAllAsRead(woody.getId(), now);

        // then
        assertThat(updated).isEqualTo(2); // 우디의 안 읽은 것 2개만
        assertThat(notificationJpaRepository.findById(unread1).orElseThrow().isRead()).isTrue();
        assertThat(notificationJpaRepository.findById(unread2).orElseThrow().getReadAt())
                .isEqualTo(now);
        assertThat(notificationJpaRepository.findById(alreadyRead).orElseThrow().getReadAt())
                .isEqualTo(alreadyReadAt); // 이미 읽은 건 시각 유지
        assertThat(notificationJpaRepository.findById(buzzUnread).orElseThrow().isRead())
                .isFalse(); // 버즈 건 그대로
    }

    private User saveUser(String email, String nickname) {
        User user = User.create(email, nickname, "monew1!"); // 실제 User 생성 방식으로
        em.persist(user);
        return user;
    }

    // createdAt은 JPA Auditing이 저장 시각으로 채우므로, 저장 후 DB 값을 직접 바꿔 원하는 시각으로 맞춘다
    private UUID saveNotification(User user, Instant createdAt, boolean read) {
        Notification n =
                Notification.create(user, ResourceType.COMMENT, UUID.randomUUID(), "테스트 알림");
        if (read) {
            n.markAsRead(LocalDateTime.now());
        }
        em.persist(n);
        em.flush();
        jdbcTemplate.update(
                "UPDATE notifications SET created_at = ? WHERE id = ?",
                Timestamp.from(createdAt),
                n.getId());
        return n.getId();
    }

    private void clearContext() {
        em.flush();
        em.clear();
    }
}
