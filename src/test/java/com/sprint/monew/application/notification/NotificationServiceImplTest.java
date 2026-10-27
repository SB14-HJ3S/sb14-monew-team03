package com.sprint.monew.application.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

import com.sprint.monew.adapter.out.notification.NotificationJpaRepository;
import com.sprint.monew.application.notification.provided.dto.NotificationQuery;
import com.sprint.monew.domain.notification.entity.Notification;
import com.sprint.monew.domain.user.entity.User;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.pagination.PageResult;
import com.sprint.monew.global.type.ResourceType;
import com.sprint.monew.support.IntegrationTest;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@IntegrationTest
@Transactional
class NotificationServiceImplTest {

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
    void createdAt이_같은_알림이_페이지_경계에_걸려도_누락이나_중복이_없다() {
        // 같은 시각 3개 + 이전 시각 2개
        List<UUID> expected = new ArrayList<>();
        expected.add(saveNotification(woody, T, false));
        expected.add(saveNotification(woody, T, false));
        expected.add(saveNotification(woody, T, false));
        expected.add(saveNotification(woody, T.minusSeconds(60), false));
        expected.add(saveNotification(woody, T.minusSeconds(120), false));
        clearContext();

        // limit=2로 끝까지 넘기면서 모은다 (경계가 같은 시각 3개 사이에 걸림)
        List<UUID> collected = new ArrayList<>();
        String cursor = null;
        Instant after = null;
        boolean hasNext = true;
        int guard = 0;

        while (hasNext && guard++ < 10) {
            NotificationQuery query = NotificationQuery.of(cursor, after, 2);
            List<Notification> fetched =
                    notificationJpaRepository.findByQuery(woody.getId(), query);
            PageResult<Notification> page =
                    PageResult.of(
                            fetched,
                            0,
                            query,
                            n -> n.getId().toString(),
                            Notification::getCreatedAt);

            page.content().forEach(n -> collected.add(n.getId()));
            cursor = page.nextCursor();
            after = page.nextAfter();
            hasNext = page.hasNext();
        }

        assertThat(collected).doesNotHaveDuplicates();
        assertThat(collected).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    void 읽은_알림과_다른_사용자_알림은_제외한다() {
        UUID unread = saveNotification(woody, T, false);
        saveNotification(woody, T, true); // 읽은 알림
        saveNotification(buzz, T, false); // 다른 사용자 알림
        clearContext();

        List<Notification> result =
                notificationJpaRepository.findByQuery(
                        woody.getId(), NotificationQuery.of(null, null, 10));

        assertThat(result).extracting(Notification::getId).containsExactly(unread);
        assertThat(notificationJpaRepository.countByQuery(woody.getId(), null)).isEqualTo(1);
    }

    @Test
    void limit보다_하나_더_조회해서_다음_페이지_유무를_판단한다() {
        saveNotification(woody, T, false);
        saveNotification(woody, T.minusSeconds(60), false);
        saveNotification(woody, T.minusSeconds(120), false);
        clearContext();

        assertThat(
                        notificationJpaRepository.findByQuery(
                                woody.getId(), NotificationQuery.of(null, null, 2)))
                .hasSize(3); // limit + 1 → hasNext = true
        assertThat(
                        notificationJpaRepository.findByQuery(
                                woody.getId(), NotificationQuery.of(null, null, 3)))
                .hasSize(3); // 딱 맞음 → hasNext = false
    }

    @Test
    void 최신순으로_정렬하고_시각이_같으면_id_내림차순이다() {
        saveNotification(woody, T.minusSeconds(60), false);
        saveNotification(woody, T, false);
        saveNotification(woody, T, false);
        clearContext();

        List<Notification> result =
                notificationJpaRepository.findByQuery(
                        woody.getId(), NotificationQuery.of(null, null, 10));

        assertThat(result)
                .extracting(Notification::getCreatedAt)
                .containsExactly(T, T, T.minusSeconds(60));
        assertThat(result.get(0).getId()).isGreaterThan(result.get(1).getId());
    }

    @Test
    void cursor와_after_중_하나만_있으면_예외() {
        assertThatThrownBy(
                        () ->
                                notificationJpaRepository.findByQuery(
                                        woody.getId(),
                                        NotificationQuery.of(
                                                UUID.randomUUID().toString(), null, 2)))
                .isInstanceOf(MonewException.class);
    }

    @Test
    void cursor가_UUID_형식이_아니면_예외() {
        assertThatThrownBy(
                        () ->
                                notificationJpaRepository.findByQuery(
                                        woody.getId(), NotificationQuery.of("abc", T, 2)))
                .isInstanceOf(MonewException.class);
    }

    // ===== 도우미 =====

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

    // DB를 직접 바꿨으니 영속성 컨텍스트에 남은 옛 값을 비운다
    private void clearContext() {
        em.flush();
        em.clear();
    }
}
