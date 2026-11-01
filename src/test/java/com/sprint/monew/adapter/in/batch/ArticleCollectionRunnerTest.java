package com.sprint.monew.adapter.in.batch;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.sprint.monew.application.article.provided.ArticleCollectionUseCase;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ArticleCollectionRunnerTest {
    @Mock private ArticleCollectionUseCase useCase;
    @InjectMocks private ArticleCollectionRunner runner;

    @Test
    @DisplayName("한 번의 실행 요청은 기사 수집 유스케이스를 한 번만 호출한다.")
    void givenCollectionRequest_whenRunCollection_thenInvokeUseCaseOnce() {
        // given
        // when
        runner.runCollection();
        // then
        verify(useCase, times(1)).collectArticles();
        verifyNoMoreInteractions(useCase);
    }

    @Test
    @DisplayName("수집 실패를 처리하여 스케줄러 호출자에게 예외를 전파하지 않는다.")
    void givenCollectionFailure_whenRunCollection_thenHandleFailure() {
        // given
        RuntimeException failure = new IllegalStateException("collection failed");
        doThrow(failure).when(useCase).collectArticles();
        // when
        Throwable thrown = catchThrowable(runner::runCollection);
        // then
        assertThat(thrown).isNull();
        verify(useCase).collectArticles();
    }
}
