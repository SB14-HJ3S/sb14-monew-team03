package com.sprint.monew.domain.interest.entity;

import ch.qos.logback.core.util.StringUtil;
import com.sprint.monew.global.entity.BaseEntity;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.InterestErrorType;
import jakarta.persistence.*;
import java.util.Collections;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

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
        if (StringUtil.isNullOrEmpty(keyword)) {
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
