package com.sprint.monew.global.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 모든 엔티티의 공통 부모 — id(UUID)와 createdAt(생성일)을 물려준다.
 *
 * <p>물리 삭제(DB에서 바로 지움)하는 엔티티는 이걸 바로 상속한다. 예: 관심사, 알림, 구독·좋아요처럼 지우면 끝인 데이터
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    // UUID
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    // 생성일 — 저장할 때 JpaAuditingConfig가 자동으로 채움 (직접 넣지 않음)
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
