package com.sprint.monew.domain.interest.entity;

import com.sprint.monew.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "interest_keywords")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterestKeyword extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "interest_id", nullable = false)
    private Interest interest;

    @Column(name = "keyword", nullable = false)
    private String keyword;

    private InterestKeyword(Interest interest, String keyword) {
        this.interest = interest;
        this.keyword = keyword;
    }

    public static InterestKeyword create(Interest interest, String keyword) {
        return new InterestKeyword(interest, keyword);
    }

    public void changeKeyword(String keyword) {
        this.keyword = keyword;
    }
}
