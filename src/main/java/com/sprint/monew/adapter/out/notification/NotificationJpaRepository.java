package com.sprint.monew.adapter.out.notification;

import com.sprint.monew.domain.notification.entity.Notification;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationJpaRepository
        extends JpaRepository<Notification, UUID>, NotificationQueryRepository {
    @Modifying(clearAutomatically = true)
    @Query(
            """
        update Notification n
        set n.isRead = true, n.readAt = :readAt
        where n.user.id = :userId and n.isRead = false
        """)
    int markAllAsRead(@Param("userId") UUID userId, @Param("readAt") LocalDateTime readAt);
}
