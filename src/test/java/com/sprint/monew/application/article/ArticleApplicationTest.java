package com.sprint.monew.application.article;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.sprint.monew.application.article.event.ArticleSoftDeletedEvent;
import com.sprint.monew.application.article.required.ArticleEventPublisherPort;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.ArticleErrorType;
import com.sprint.monew.global.type.ArticleSourceType;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ArticleApplicationTest {

    @Mock private ArticleService articleService;
    @Mock private ArticleViewStatusService articleViewStatusService;
    @Mock private ArticleEventPublisherPort eventPublisher;

    @InjectMocks private ArticleApplication articleApplication;

    @Test
    @DisplayName("출처 목록을 조회하면 등록된 출처를 전부 돌려준다")
    void whenFindSourceTypes_thenReturnAllSources() {
        List<ArticleSourceType> sources = articleApplication.findSourceTypes();

        assertThat(sources).containsExactly(ArticleSourceType.values());
    }

    @Test
    @DisplayName("뉴스 기사를 논리 삭제하면 서비스에 논리 삭제를 맡긴 뒤 논리 삭제 이벤트를 보낸다")
    void givenArticleId_whenSoftDeleteArticle_thenDelegateAndPublishEvent() {
        UUID articleId = UUID.randomUUID();

        articleApplication.softDeleteArticle(articleId);

        InOrder inOrder = inOrder(articleService, eventPublisher);
        inOrder.verify(articleService).softDelete(articleId);
        inOrder.verify(eventPublisher).publish(new ArticleSoftDeletedEvent(articleId));
    }

    @Test
    @DisplayName("논리 삭제할 뉴스 기사가 없으면 예외가 나고 논리 삭제 이벤트를 보내지 않는다")
    void givenNoArticle_whenSoftDeleteArticle_thenThrowAndNotPublish() {
        UUID articleId = UUID.randomUUID();
        willThrow(new MonewException(ArticleErrorType.ARTICLE_NOT_FOUND))
                .given(articleService)
                .softDelete(articleId);

        assertThatThrownBy(() -> articleApplication.softDeleteArticle(articleId))
                .isInstanceOf(MonewException.class);
        verify(eventPublisher, never()).publish(any(ArticleSoftDeletedEvent.class));
    }
}
