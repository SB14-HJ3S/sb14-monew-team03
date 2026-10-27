package com.sprint.monew.application.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

import com.sprint.monew.adapter.out.notification.NotificationJpaRepository;
import com.sprint.monew.application.notification.provided.NotificationUseCase;
import com.sprint.monew.domain.notification.entity.Notification;
import com.sprint.monew.domain.user.entity.User;
import com.sprint.monew.global.context.RequestIdContext;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.type.ResourceType;
import com.sprint.monew.support.IntegrationTest;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

@IntegrationTest
class NotificationApplicationTest {

    @Autowired NotificationUseCase notificationUseCase;
    @Autowired NotificationJpaRepository notificationJpaRepository;
    @Autowired TransactionTemplate transactionTemplate;
    @Autowired EntityManager em;

    @AfterEach
    void tearDown() {
        RequestIdContext.clear(); // 실제 메서드 이름으로
        transactionTemplate.executeWithoutResult(
                status -> {
                    em.createQuery("delete from Notification").executeUpdate();
                    em.createQuery("delete from User").executeUpdate();
                });
    }

    @Test
    void 단건_확인하면_DB에_확인_상태가_저장된다() {
        // given
        UUID[] ids =
                transactionTemplate.execute(
                        status -> {
                            User woody =
                                    User.create("woody@monew.com", "우디", "monew1!"); // 실제 생성 방식으로
                            em.persist(woody);
                            Notification n =
                                    Notification.create(
                                            woody,
                                            ResourceType.COMMENT,
                                            UUID.randomUUID(),
                                            "테스트 알림");
                            em.persist(n);
                            return new UUID[] {woody.getId(), n.getId()};
                        });
        UUID userId = ids[0];
        UUID notificationId = ids[1];
        RequestIdContext.set(userId); // 실제 메서드 이름으로

        // when
        notificationUseCase.markAsRead(notificationId);

        // then: 새로 읽어서 DB에 반영됐는지 확인
        Notification reloaded = notificationJpaRepository.findById(notificationId).orElseThrow();
        assertThat(reloaded.isRead()).isTrue();
        assertThat(reloaded.getReadAt()).isNotNull();
    }

    @Test
    void 없는_사용자가_전체_확인하면_예외() {
        RequestIdContext.set(UUID.randomUUID()); // 실제 메서드 이름으로

        assertThatThrownBy(() -> notificationUseCase.markAllAsRead())
                .isInstanceOf(MonewException.class);
    }
}
