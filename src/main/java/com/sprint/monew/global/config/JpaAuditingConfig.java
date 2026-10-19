package com.sprint.monew.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

// JPA Auditing 켜기 — BaseEntity를 상속한 엔티티는 저장할 때 createdAt(생성 시각)이 자동으로 채워진다
// 엔티티마다 createdAt = Instant.now()를 직접 쓰지 않아도 되게 하려고 넣음 (빠뜨리거나 시각이 제각각인 실수 방지)
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {}
