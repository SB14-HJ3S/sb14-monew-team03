package com.sprint.monew.adapter.in.batch;

import com.sprint.monew.application.article.provided.ArticleCollectionUseCase;
import com.sprint.monew.global.exception.MonewException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionException;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleCollectionRunner {

    private final ArticleCollectionUseCase articleCollectionUseCase;

    public void runCollection() {
        // 얘로 바꿀거임
        articleCollectionUseCase.collectArticles();

        try {
            articleCollectionUseCase.collectArticles();
        } catch (MonewException e) {
            log.error("기사 수집 실패: errorType={}", e.getErrorType(), e);
        } catch (DataAccessException | TransactionException e) {
            log.error("기사 저장 실패", e);
        } catch (RuntimeException e) {
            log.error("기사 수집 중 예상하지 못한 오류", e);
        }
    }
}
