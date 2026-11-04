package com.sprint.monew.application.article;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.monew.domain.article.entity.Article;
import com.sprint.monew.domain.article.entity.ArticleInterest;
import com.sprint.monew.domain.interest.entity.Interest;
import com.sprint.monew.global.type.ArticleSourceType;
import com.sprint.monew.support.IntegrationTest;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class ArticleInterestIntegrationTest {

    @Autowired private ArticleInterestService service;
    @Autowired private EntityManager em;

    @Test
    @DisplayName("같은 기사와 관심사의 연결은 재사용하고 다른 관심사에는 새 연결을 만든다.")
    void givenArticleAndInterests_whenGetOrCreateRepeatedly_thenReuseOnlyMatchingLink() {
        // given
        Article article =
                Article.create(
                        ArticleSourceType.NAVER,
                        "https://example.com/" + UUID.randomUUID(),
                        "AI 기사",
                        LocalDateTime.of(2026, 10, 23, 12, 0),
                        "반도체와 인공지능");
        Interest ai = Interest.create("AI", List.of("인공지능"));
        Interest chip = Interest.create("반도체", List.of("반도체"));
        em.persist(article);
        em.persist(ai);
        em.persist(chip);
        em.flush();

        // when
        boolean created = service.createIfAbsent(article, ai);
        em.flush();
        UUID createdId =
                em.createQuery(
                                "select ai.id from ArticleInterest ai where ai.article.id = :id",
                                UUID.class)
                        .setParameter("id", article.getId())
                        .getSingleResult();
        em.clear();

        Article managedArticle = em.find(Article.class, article.getId());
        boolean existing =
                service.createIfAbsent(managedArticle, em.find(Interest.class, ai.getId()));
        boolean other =
                service.createIfAbsent(managedArticle, em.find(Interest.class, chip.getId()));
        em.flush();

        // then
        assertThat(created).isTrue();
        assertThat(existing).isFalse();
        assertThat(other).isTrue();
        List<ArticleInterest> persistedLinks =
                em.createQuery(
                                "select ai from ArticleInterest ai where ai.article.id = :id",
                                ArticleInterest.class)
                        .setParameter("id", article.getId())
                        .getResultList();
        assertThat(persistedLinks)
                .extracting(link -> link.getInterest().getId())
                .containsExactlyInAnyOrder(ai.getId(), chip.getId());
        assertThat(persistedLinks)
                .filteredOn(link -> link.getInterest().getId().equals(ai.getId()))
                .extracting(ArticleInterest::getId)
                .containsExactly(createdId);
    }
}
