package com.sprint.monew.application.notification.required;

import com.sprint.monew.domain.notification.entity.Notification;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepositoryPort {

    Notification save(Notification entity);

    void saveAll(List<Notification> notifications);

    Optional<Notification> findById(UUID id);

    List<Notification> findAll();

    void deleteById(UUID id);
}
