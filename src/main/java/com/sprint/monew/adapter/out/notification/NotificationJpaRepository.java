package com.sprint.monew.adapter.out.notification;

import com.sprint.monew.domain.notification.entity.Notification;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationJpaRepository
        extends JpaRepository<Notification, UUID>, NotificationQueryRepository {}
