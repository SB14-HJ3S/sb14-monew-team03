package com.sprint.monew.application.article;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.sprint.monew.application.article.required.ArticleRepositoryPort;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.ArticleErrorType;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ArticleServiceImplTest {

    @Mock private ArticleRepositoryPort repository;

    @InjectMocks private ArticleServiceImpl articleService;

    @Test
    @DisplayName("삭제되지 않은 뉴스 기사를 논리 삭제하면 삭제 시각을 넣는 쿼리를 실행한다")
    void givenActiveArticle_whenSoftDelete_thenUpdateDeletedAt() {
        UUID articleId = UUID.randomUUID();
        given(repository.softDeleteIfActive(eq(articleId), any(Instant.class))).willReturn(1);

        articleService.softDelete(articleId);

        verify(repository).softDeleteIfActive(eq(articleId), any(Instant.class));
    }

    @Test
    @DisplayName("없거나 이미 논리 삭제된 뉴스 기사를 논리 삭제하면 ARTICLE_NOT_FOUND 예외가 난다")
    void givenNoActiveArticle_whenSoftDelete_thenThrowArticleNotFound() {
        UUID articleId = UUID.randomUUID();
        given(repository.softDeleteIfActive(eq(articleId), any(Instant.class))).willReturn(0);

        assertThatThrownBy(() -> articleService.softDelete(articleId))
                .isInstanceOf(MonewException.class)
                .extracting("errorType")
                .isEqualTo(ArticleErrorType.ARTICLE_NOT_FOUND);
    }
}
