package com.sprint.monew.domain.interest.entity;

import com.sprint.monew.global.entity.BaseEntity;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.InterestErrorType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

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
    private Long subscriberCount;

    @OneToMany(
            mappedBy = "interest",
            fetch = FetchType.LAZY,
            cascade = {CascadeType.PERSIST, CascadeType.REMOVE},
            orphanRemoval = true)
    private final List<InterestKeyword> keywords = new ArrayList<>();

    private Interest(String name) {
        if (!StringUtils.hasText(name)) {
            throw new MonewException(
                    InterestErrorType.INTEREST_NAME_REQUIRED,
                    Collections.singletonMap("name", name));
        }

        this.name = name;
        this.subscriberCount = 0L;
    }

    public static Interest create(String name, List<String> keywords) {
        if (CollectionUtils.isEmpty(keywords)) {
            throw new MonewException(
                    InterestErrorType.INTEREST_KEYWORD_REQUIRED,
                    Collections.singletonMap("keywords", keywords));
        }
        Interest creating = new Interest(name);
        List<InterestKeyword> interestKeywords =
                keywords.stream()
                        .map(keyword -> InterestKeyword.create(creating, keyword))
                        .toList();
        creating.keywords.addAll(interestKeywords);
        return creating;
    }
}
