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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Check;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Entity
@Table(
        name = "interests",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"name"})})
@Getter
@Check(constraints = "subscriber_count >= 0")
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

    public List<String> getKeywordNames() {
        return keywords.stream().map(InterestKeyword::getKeyword).toList();
    }

    public Interest updateKeywords(List<String> newKeywords) {
        validateInterestKeywords(newKeywords);

        List<InterestKeyword> addingKeywords = getAddingKeywords(newKeywords);

        removeKeywordsIfNotInNewKeywords(newKeywords);
        keywords.addAll(addingKeywords);
        return this;
    }

    private List<InterestKeyword> getAddingKeywords(List<String> newKeywords) {
        List<String> existingKeywords = getKeywordNames();

        return newKeywords.stream()
                .filter(newKeyword -> !existingKeywords.contains(newKeyword))
                .map(keyword -> InterestKeyword.create(this, keyword))
                .toList();
    }

    private void removeKeywordsIfNotInNewKeywords(List<String> newKeywords) {
        keywords.removeIf(existing -> !newKeywords.contains(existing.getKeyword()));
    }

    private static void validateInterestKeywords(List<String> newKeywords) {
        validateKeywordsLength(newKeywords);
        validateKeywordsInputUnique(newKeywords);
    }

    private static void validateKeywordsLength(List<String> newKeywords) {
        if (CollectionUtils.isEmpty(newKeywords) || newKeywords.size() > MAX_KEYWORDS_SIZE) {
            throw new MonewException(
                    InterestErrorType.INVALID_INTEREST_KEYWORD,
                    Collections.singletonMap("keywords", newKeywords));
        }
    }

    private static void validateKeywordsInputUnique(List<String> newKeywords) {
        Set<String> seen = new HashSet<>();
        boolean isDuplicated = newKeywords.stream().anyMatch(keyword -> !seen.add(keyword));

        if (isDuplicated) {
            throw new MonewException(
                    InterestErrorType.INVALID_INTEREST_KEYWORD,
                    Map.of("keywords", "키워드 생성/수정 시 같은 중복되는 키워드 이름을 요청할 수 없습니다."));
        }
    }
}
