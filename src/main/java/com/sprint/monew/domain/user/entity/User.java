package com.sprint.monew.domain.user.entity;

import com.sprint.monew.global.entity.BaseSoftDeletableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

// JPA Auditing은 엔티티가 하나도 없으면 앱이 뜨지 않아서 미리 둔 빈 엔티티 — 필드는 사용자 담당자가 채운다
@Entity
@Table(name = "users")
public class User extends BaseSoftDeletableEntity {}
