package com.sprint.monew.application.article;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sprint.monew.application.article.provided.ArticleUseCase;
import com.sprint.monew.domain.article.entity.Article;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.ArticleErrorType;
import com.sprint.monew.global.type.ArticleSourceType;
import com.sprint.monew.support.IntegrationTest;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class ArticleSoftDeleteIntegrationTest {

    @Autowired private ArticleUseCase articleUseCase;
    @Autowired private EntityManager em;

    private Article savedArticle(String sourceUrl) {
        Article article =
                Article.create(
                        ArticleSourceType.NAVER, sourceUrl, "기사 제목", LocalDateTime.now(), "기사 요약");
        em.persist(article);
        em.flush();
        em.clear();
        return article;
    }

    @Test
    @DisplayName("뉴스 기사를 논리 삭제하면 삭제 시각이 DB에 저장되고 단건 조회에서 404가 난다")
    void givenArticle_whenSoftDelete_thenDeletedAtSavedAndNotFound() {
        Article article = savedArticle("https://news.example.com/soft");

        articleUseCase.softDeleteArticle(article.getId());

        assertThat(em.find(Article.class, article.getId()).isDeleted()).isTrue();
        assertThatThrownBy(() -> articleUseCase.findById(article.getId()))
                .isInstanceOf(MonewException.class)
                .extracting("errorType")
                .isEqualTo(ArticleErrorType.ARTICLE_NOT_FOUND);
    }

    @Test
    @DisplayName("이미 논리 삭제된 뉴스 기사를 다시 논리 삭제하면 ARTICLE_NOT_FOUND 예외가 난다")
    void givenDeletedArticle_whenSoftDeleteAgain_thenThrowArticleNotFound() {
        Article article = savedArticle("https://news.example.com/soft-again");
        articleUseCase.softDeleteArticle(article.getId());

        assertThatThrownBy(() -> articleUseCase.softDeleteArticle(article.getId()))
                .isInstanceOf(MonewException.class)
                .extracting("errorType")
                .isEqualTo(ArticleErrorType.ARTICLE_NOT_FOUND);
    }
}
