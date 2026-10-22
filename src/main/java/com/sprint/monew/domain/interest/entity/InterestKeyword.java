package com.sprint.monew.domain.interest.entity;

import com.sprint.monew.global.entity.BaseEntity;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.InterestErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Collections;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.util.StringUtils;

@Entity
@Table(name = "interest_keywords")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterestKeyword extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "interest_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Interest interest;

    @Column(name = "keyword", nullable = false)
    private String keyword;

    private InterestKeyword(Interest interest, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            throw new MonewException(
                    InterestErrorType.INTEREST_KEYWORD_REQUIRED,
                    Collections.singletonMap("keyword", keyword));
        }

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
