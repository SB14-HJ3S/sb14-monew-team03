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
    public static final int MAX_NAME_LENGTH = 50;
    public static final int MAX_KEYWORDS_SIZE = 10;

    @Column(name = "name", nullable = false, length = MAX_NAME_LENGTH)
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
        if (!StringUtils.hasText(name) || name.length() > MAX_NAME_LENGTH) {
            throw new MonewException(
                    InterestErrorType.INVALID_INTEREST_NAME,
                    Collections.singletonMap("name", name));
        }

        this.name = name;
        this.subscriberCount = 0L;
    }

    public static Interest create(String name, List<String> keywords) {
        validateInterestKeywords(keywords);
        Interest creating = new Interest(name);
        List<InterestKeyword> interestKeywords =
                keywords.stream()
                        .map(keyword -> InterestKeyword.create(creating, keyword))
                        .toList();
        creating.keywords.addAll(interestKeywords);
        return creating;
    }

    public Interest updateKeywords(List<String> newKeywords) {
        validateInterestKeywords(newKeywords);

        List<InterestKeyword> addingKeywords = getAddingKeywords(newKeywords);

        removeKeywordsIfNotInNewKeywords(newKeywords);
        keywords.addAll(addingKeywords);
        return this;
    }

    private List<InterestKeyword> getAddingKeywords(List<String> newKeywords) {
        List<String> existingKeywords = keywords.stream().map(InterestKeyword::getKeyword).toList();

        return newKeywords.stream()
                .filter(newKeyword -> !existingKeywords.contains(newKeyword))
                .map(keyword -> InterestKeyword.create(this, keyword))
                .toList();
    }

    private void removeKeywordsIfNotInNewKeywords(List<String> newKeywords) {
        keywords.removeIf(existing -> !newKeywords.contains(existing.getKeyword()));
    }

    private static void validateInterestKeywords(List<String> keywords) {
        if (CollectionUtils.isEmpty(keywords) || keywords.size() > MAX_KEYWORDS_SIZE) {
            throw new MonewException(
                    InterestErrorType.INVALID_INTEREST_KEYWORD,
                    Collections.singletonMap("keywords", keywords));
        }
    }
}
