package com.sprint.monew.adapter.out.notification;

import com.sprint.monew.application.notification.provided.dto.NotificationQuery;
import com.sprint.monew.application.notification.required.NotificationRepositoryPort;
import com.sprint.monew.domain.notification.entity.Notification;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class NotificationRepositoryAdapter implements NotificationRepositoryPort {

    private final NotificationJpaRepository jpaRepository;

    @Override
    public Notification save(Notification entity) {
        return jpaRepository.save(entity);
    }

    @Override
    public Optional<Notification> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<Notification> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public void saveAll(List<Notification> notifications) {
        jpaRepository.saveAll(notifications);
    }

    @Override
    public List<Notification> findByQuery(UUID userId, NotificationQuery query) {
        return jpaRepository.findByQuery(userId, query);
    }

    @Override
    public long countByQuery(UUID userId, NotificationQuery query) {
        return jpaRepository.countByQuery(userId, query);
    }

    @Override
    public int markAllAsRead(UUID userId, LocalDateTime readAt) {
        return jpaRepository.markAllAsRead(userId, readAt);
    }

    @Override
    public int deleteReadBefore(LocalDateTime threshold) {
        return jpaRepository.deleteReadBefore(threshold);
    }
}
