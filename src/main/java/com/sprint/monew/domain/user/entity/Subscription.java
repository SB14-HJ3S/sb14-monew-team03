package com.sprint.monew.domain.user.entity;

import com.sprint.monew.domain.interest.entity.Interest;
import com.sprint.monew.global.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(
        name = "subscriptions",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"user_id", "interest_id"})})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Subscription extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "interest_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Interest interest;

    private Subscription(User user, Interest interest) {
        this.user = user;
        this.interest = interest;
    }

    public static Subscription create(User user, Interest interest) {
        return new Subscription(user, interest);
    }
}
