package com.sprint.monew.global.entity;

import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import lombok.Getter;

/**
 * 논리 삭제(데이터는 남기고 deletedAt에 삭제 시각만 기록)하는 엔티티의 부모 — 사용자, 뉴스기사, 댓글
 *
 * <p>BaseEntity를 상속해서 id·createdAt도 같이 물려받는다.
 */
@Getter
@MappedSuperclass
public abstract class BaseSoftDeletableEntity extends BaseEntity {

    private Instant deletedAt; // 삭제한 시각 — 비어 있으면 아직 삭제 안 된 데이터

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void softDelete() {
        if (isDeleted()) {
            return;
        }
        this.deletedAt = Instant.now();
    }
}
