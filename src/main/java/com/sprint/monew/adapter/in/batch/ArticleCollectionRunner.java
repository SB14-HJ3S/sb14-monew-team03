package com.sprint.monew.adapter.in.batch;

import com.sprint.monew.application.article.provided.ArticleCollectionUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleCollectionRunner {

    private final ArticleCollectionUseCase articleCollectionUseCase;

    public void runCollection() {
        //        try {
        //            // 예시 코드 - 나중에 배치로 채우기
        //            List<InterestKeywordResult> interest =
        // articleCollectionUseCase.findValidInterestKeywords();
        //
        //            List<CollectedArticle> rssArticles =
        // articleCollectionUseCase.collectRssArticles(ArticleSourceType.HANKYUNG);
        //
        //            for (InterestKeywordResult interestKeywordResult : interest) {
        //                for (String keyword : interestKeywordResult.interest().getKeywordNames())
        // {
        //                    List<CollectedArticle> naverArticles =
        // articleCollectionUseCase.collectNaverArticles(interestKeywordResult.keyword());
        //                }
        //                // rssArticles?????
        //            }
        //
        //        } catch (MonewException e) {
        //            log.error("기사 수집 실패: errorType={}", e.getErrorType(), e);
        //        } catch (DataAccessException | TransactionException e) {
        //            log.error("기사 저장 실패", e);
        //        } catch (RuntimeException e) {
        //            log.error("기사 수집 중 예상하지 못한 오류", e);
        //        }
    }
}
