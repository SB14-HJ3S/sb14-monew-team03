package com.sprint.monew.domain.interest.entity;

import com.sprint.monew.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "interests",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"name"})})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Interest extends BaseEntity {
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "subscriber_count", nullable = false)
    private int subscriberCount;

    private Interest(String name) {
        this.name = name;
        this.subscriberCount = 0;
    }

    public static Interest create(String name) {
        return new Interest(name);
    }
}
