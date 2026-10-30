# 기사 수집, 알림 생성 메커니즘 정리

## 전체흐름:

```text
정각
  ↓
Scheduler.collect()
  ↓
Runner.runCollection()
  ↓
수집 대상 관심사 조회
  ↓
관심사별 반복 ──────────────────────────────────────┐
  │                                              │
  ├─ 모든 키워드·페이지의 네이버 기사 조회                │
  │                                              │
  └─ saveArticlesAndLink()                       │
       ┌─ 트랜잭션 A 시작                           │
       │  기사 조회·저장                            │
       │  관심사 연결 조회·저장                       │
       │  신규 기사 수 집계                          │
       │  ArticlesSavedEvent 발행                 │
       └─ 커밋 성공                                │
            ├─ Runner는 다음 관심사로 ───────────────┘
            │
            └─ AFTER_COMMIT + @Async
                 ↓
               NotificationEventListener
                 ↓
               NotificationApplication
                 ┌─ 트랜잭션 B 시작
                 │  구독자·활성 사용자 조회
                 │  사용자별 알림 저장
                 └─ 커밋
```

## 기사 수집

1. 정각에 스케줄러 실행해서, Runner 실행
2. Runner 내부에서는:
    1. 수집할 관심사 조회: `collectionUseCase.findCollectionInterests()`
        * 내부적으로는 InterestService.findSubscribedWithKeywords() 실행
    2. 관심사 하나마다 그 안의 모든 키워드로 네이버 조회
        1. 관심사마다 `collectionUseCase.collectNaverArticles(interest)` 호출

        * 내부적으로 각 키워드마다 NaverNewsAdapter.search(keyword) 실행. 최대 100개 기사씩, 최대 10페이지 요청.
    3. 관심사 한개에 대한 모든 조회 끝
    4. 관심사에 대한 기사 저장 트랜잭션 시작: `collectionUseCase.saveArticlesAndLink(articles, interest)`
        1. 기사마다 기존에 저장한 적이 있는 기사인지 확인하고 저장
            * 내부적으로 `ArticleService.findBySourceUrl(url)` 실행
        2. 기사와 관심사를 연결
            * `ArticleInterestService.getOrCreateArticleInterest(article, interest)`
        3. 신규 기사가 있으면 publisher로 이벤트 발행
            * `articleService.publishArticleSaved(interestId, interestName, count)`
        4. 종료 -> 트랜잭션 커밋

## 알림

1. 알림 리스너가 별도의 스레드에서 실행되고 있음
2. 한 관심사에 대한 기사 수집이 완료되면 `onArticlesSaved(event)` 실행됨
3. 알림 생성 트랜잭션 시작: `NotificationApplication.createArticleNotifications(interestId, InterestName, count)`
    * 내부적으로:
        * 구독자 ID 조회: `SubscriptionService.findSubscriberIds(interestId)`
        * 활성 사용자 조회: `UserService.getActiveUsers(subscriberIds)`
        * 실제 생성: `NotificationService.createArticleNotifications(...)`
4. 알림 생성 트랜잭션 종료



