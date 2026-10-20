package com.sprint.monew.domain.notification.entity;

import com.sprint.monew.domain.user.entity.User;
import com.sprint.monew.global.entity.BaseEntity;
import com.sprint.monew.global.type.ResourceType;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notifications")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false)
    private ResourceType resourceType;

    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    @Column(name = "is_read", nullable = false)
    private boolean isRead;

    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    private Notification(User user, ResourceType resourceType, UUID resourceId, String content) {
        this.user = user;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.content = content;
        this.isRead = false;
    }

    public static Notification create(
            User user, ResourceType resourceType, UUID resourceId, String content) {
        return new Notification(user, resourceType, resourceId, content);
    }

    public void markAsRead(LocalDateTime readAt) {
        if (!isRead) {
            this.isRead = true;
            this.readAt = readAt;
        }
    }
}
