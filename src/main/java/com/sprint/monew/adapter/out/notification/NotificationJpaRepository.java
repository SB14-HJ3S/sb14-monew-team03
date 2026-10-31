package com.sprint.monew.adapter.out.notification;

import com.sprint.monew.domain.notification.entity.Notification;
import com.sprint.monew.global.type.ResourceType;
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

    @Modifying(clearAutomatically = true)
    @Query(
            """
            delete from Notification n
            where n.isRead = true and n.readAt < :threshold
            """)
    int deleteReadBefore(@Param("threshold") LocalDateTime threshold);

    // 비우기 전에 먼저 반영한다 — 같은 트랜잭션에서 앞서 지운 댓글 같은 변경이 사라지지 않게
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            """
            delete from Notification n
            where n.resourceType = :resourceType and n.resourceId = :resourceId
            """)
    int deleteByResource(
            @Param("resourceType") ResourceType resourceType, @Param("resourceId") UUID resourceId);
}
