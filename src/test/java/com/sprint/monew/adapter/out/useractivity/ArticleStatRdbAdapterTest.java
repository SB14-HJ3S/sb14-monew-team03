package com.sprint.monew.adapter.out.useractivity;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.monew.application.useractivity.required.ArticleStatReader.ArticleStat;
import com.sprint.monew.domain.article.entity.Article;
import com.sprint.monew.global.type.ArticleSourceType;
import com.sprint.monew.support.IntegrationTest;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@IntegrationTest
@Transactional
class ArticleStatRdbAdapterTest {

    @Autowired private ArticleStatRdbAdapter adapter;
    @Autowired private EntityManager em;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("기사 ID 목록으로 현재 조회 수·댓글 수를 한 번에 읽는다.")
    void givenArticles_whenFindStats_thenCurrentCounts() {
        Article a = saveArticle("https://a.com/1");
        Article b = saveArticle("https://a.com/2");
        setCounts(a.getId(), 7, 3);
        setCounts(b.getId(), 1, 0);
        em.clear();

        Map<UUID, ArticleStat> stats = adapter.findStats(List.of(a.getId(), b.getId()));

        assertThat(stats.get(a.getId())).isEqualTo(new ArticleStat(7, 3));
        assertThat(stats.get(b.getId())).isEqualTo(new ArticleStat(1, 0));
    }

    @Test
    @DisplayName("없는 기사 ID는 결과에서 빠진다.")
    void givenUnknownId_whenFindStats_thenOmitted() {
        Article a = saveArticle("https://a.com/1");

        Map<UUID, ArticleStat> stats = adapter.findStats(List.of(a.getId(), UUID.randomUUID()));

        assertThat(stats).containsOnlyKeys(a.getId());
    }

    @Test
    @DisplayName("빈 목록이면 쿼리 없이 빈 결과를 돌려준다.")
    void givenEmpty_whenFindStats_thenEmpty() {
        assertThat(adapter.findStats(List.of())).isEmpty();
    }

    private Article saveArticle(String url) {
        Article article =
                Article.create(
                        ArticleSourceType.NAVER,
                        url,
                        "제목",
                        LocalDateTime.of(2026, 11, 1, 9, 0),
                        "요약");
        em.persist(article);
        em.flush();
        return article;
    }

    // 조회 수·댓글 수는 생성 때 0이라, 테스트용으로 DB 값을 직접 바꾼다
    private void setCounts(UUID articleId, int viewCount, int commentCount) {
        jdbcTemplate.update(
                "UPDATE articles SET view_count = ?, comment_count = ? WHERE id = ?",
                viewCount,
                commentCount,
                articleId);
    }
}
