package com.sprint.monew.adapter.out.notification;

import com.sprint.monew.application.notification.required.NotificationRepositoryPort;
import com.sprint.monew.domain.notification.entity.Notification;
import com.sprint.monew.domain.user.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class NotificationRepositoryAdapter implements NotificationRepositoryPort {

    private final NotificationJpaRepository jpaRepository;

    @PersistenceContext private EntityManager em;

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
    public User getUserReference(UUID userId) {
        return em.getReference(User.class, userId);
    }
}
