package com.sprint.monew.domain.article.entity;

import com.sprint.monew.domain.interest.entity.Interest;
import com.sprint.monew.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(
        name = "article_interests",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"article_id", "interest_id"})})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArticleInterest extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "article_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Article article;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "interest_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Interest interest;

    private ArticleInterest(Article article, Interest interest) {
        this.article = article;
        this.interest = interest;
    }

    public static ArticleInterest create(Article article, Interest interest) {
        return new ArticleInterest(article, interest);
    }
}
