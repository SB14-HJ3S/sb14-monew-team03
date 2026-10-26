package com.sprint.monew.application.interest;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.monew.adapter.out.article.ArticleInterestJpaRepository;
import com.sprint.monew.adapter.out.article.ArticleJpaRepository;
import com.sprint.monew.adapter.out.interest.InterestJpaRepository;
import com.sprint.monew.adapter.out.interest.SubscriptionJpaRepository;
import com.sprint.monew.adapter.out.user.UserJpaRepository;
import com.sprint.monew.domain.article.entity.Article;
import com.sprint.monew.domain.article.entity.ArticleInterest;
import com.sprint.monew.domain.interest.entity.Interest;
import com.sprint.monew.domain.user.entity.Subscription;
import com.sprint.monew.domain.user.entity.User;
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
class InterestIntegrationTest {

    @Autowired private InterestApplication interestApplication;
    @Autowired private InterestJpaRepository interestRepository;
    @Autowired private SubscriptionJpaRepository subscriptionRepository;
    @Autowired private ArticleJpaRepository articleRepository;
    @Autowired private ArticleInterestJpaRepository articleInterestRepository;
    @Autowired private UserJpaRepository userRepository;
    @Autowired private EntityManager entityManager;

    @Test
    @DisplayName("관심사를 삭제하면 해당 관심사의 키워드, 기사 매핑과 구독만 함께 삭제한다.")
    void givenInterestWithRelations_whenDelete_thenDeleteOnlyTargetInterestAndRelations() {
        // given
        Interest target = interestRepository.save(Interest.create("삭제 대상", List.of("대상")));
        Interest retained = interestRepository.save(Interest.create("유지 대상", List.of("유지")));
        User user = userRepository.save(User.create("user@monew.com", "사용자", "password1!"));
        Article article =
                articleRepository.save(
                        Article.create(
                                ArticleSourceType.NAVER,
                                "https://news.example.com/interest-deletion",
                                "기사 제목",
                                LocalDateTime.of(2026, 10, 26, 12, 0),
                                "기사 요약"));

        subscriptionRepository.save(Subscription.create(user, target));
        subscriptionRepository.save(Subscription.create(user, retained));
        articleInterestRepository.save(ArticleInterest.create(article, target));
        articleInterestRepository.save(ArticleInterest.create(article, retained));
        assertThat(countKeywordsByInterestId(target.getId())).isEqualTo(1);
        assertThat(countKeywordsByInterestId(retained.getId())).isEqualTo(1);

        // when
        interestApplication.delete(target.getId());

        // then
        assertThat(interestRepository.existsById(target.getId())).isFalse();
        assertThat(countKeywordsByInterestId(target.getId())).isZero();

        assertThat(interestRepository.existsById(retained.getId())).isTrue();
        assertThat(countKeywordsByInterestId(retained.getId())).isEqualTo(1);
        assertThat(subscriptionRepository.count()).isEqualTo(1);
        assertThat(articleInterestRepository.count()).isEqualTo(1);
        assertThat(userRepository.existsById(user.getId())).isTrue();
        assertThat(articleRepository.existsById(article.getId())).isTrue();
    }

    private long countKeywordsByInterestId(UUID interestId) {
        return entityManager
                .createQuery(
                        "select count(k) from InterestKeyword k where k.interest.id = :interestId",
                        Long.class)
                .setParameter("interestId", interestId)
                .getSingleResult();
    }
}
