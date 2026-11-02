# 기사 수집, 알림 생성 메커니즘 정리

## 전체흐름:

```text
매시간 정각 — Asia/Seoul
  ↓
ArticleCollectionScheduler.collect()
  ↓
ArticleCollectionRunner.runCollection()
  │
  ├─ collectArticles() 첫 번째 호출 — try-catch 밖
  │
  └─ 첫 호출이 정상 반환하면 두 번째 호출 — try-catch 안

각 collectArticles() 호출
  ↓
ArticleCollectionApplication.collectArticles()
  │
  ├─ 구독자가 있는 관심사와 키워드 조회
  │
  ├─ RSS 수집 — 수집 실행당 언론사별 한 번
  │   ├─ 한국경제
  │   ├─ 조선일보
  │   └─ 연합
  │       ↓
  │     ROME 파싱 → CollectedArticle 변환
  │       ↓
  │     URL 기준 중복 제거
  │
  └─ 관심사별 반복
      │
      ├─ RSS 제목·요약에 관심사 키워드가 포함된 기사 선택
      │
      ├─ saveArticleInterestInTransaction(matchedRssArticles, interest)
      │   ┌─ 트랜잭션 A 시작 — TransactionTemplate
      │   │  기사별 URL 조회
      │   │  신규 기사 저장 / 기존 기사 재사용
      │   │  기사·관심사 연결 조회·생성
      │   │  DB 신규 저장 기사 수 집계
      │   └─ 커밋 후 RSS 신규 기사 수 반환
      │
      ├─ collectNaverArticles(interest)
      │   ├─ 관심사의 키워드별 NaverNewsPort.search(keyword)
      │   │   └─ NaverNewsAdapter
      │   │       ├─ 100건씩 최대 10페이지 요청
      │   │       └─ NaverNewsMapper → CollectedArticle 변환
      │   └─ URL 기준 중복 제거
      │
      ├─ saveArticleInterestInTransaction(naverArticles, interest)
      │   ┌─ 트랜잭션 B 시작 — TransactionTemplate
      │   │  기사별 URL 조회
      │   │  신규 기사 저장 / 기존 기사 재사용
      │   │  기사·관심사 연결 조회·생성
      │   │  DB 신규 저장 기사 수 집계
      │   └─ 커밋 후 네이버 신규 기사 수 반환
      │
      ├─ newCount = RSS 신규 기사 수 + 네이버 신규 기사 수
      │
      ├─ newCount > 0
      │   └─ ArticlesSavedEvent 발행 — 관심사당 한 번
      │       └─ 현재는 트랜잭션 밖에서 발행
      │           → AFTER_COMMIT 리스너 실행 안 됨
      │
      └─ 다음 관심사로 진행
```

## 기사 수집

1. 정각에 스케줄러 실행해서, Runner 실행
    2. `ArticleCollectionUsecase.collectArticles()` 실행
        1. 수집할 관심사 조회: `InterestService.findSubscribedWithKeywords()` 실행
        2. RSS 기사 수집: `ArticleCollectionUseCase.collectRssArticles()` 실행
            1. 한국경제, 조선, 연합 순서로 `RssNewsPort.collect(sourceType)` 실행
            2. 내부적으로 RssNewsAdapter가 다음을 수행:
                1. RestClient로 RSS 요청
                2. XmlReader, SyndFeedInput으로 XML 파싱
                3. RssNewsMapper로 CollectedArticle로 변환
                    * URL, 제목, 발행일 검증. HTML 태그 제거
        2. 관심사 하나에 대해:
            1. RSS 기사를 선별: **(선수집, 후선별)**
                1. `matchesInterest(article, interest)` 실행
                    1. 제목, 요약을 합쳐서 소문자로 변환
                    2. 관심사의 키워드 중 하나라도 있으면 true, else false
            2. Naver 기사 수집: **(수집 & 선별 동시에)**
                1. `collectNaverArticles(interest)` 호출
                2. 내부적으로 NaverNewsAdapter가 최대 100건씩, 최대 10페이지를 요청한다
                3. 관심사 내 검색 결과는 URL 기준으로 중복 제거
            3. 기사 저장, 관심사와 연결 **@Transactional**: `saveArticleInterestInTransaction(articles, interest)` 호출
                1. `TransactionTemplate.execute`로 저장. 내부적으로 `saveArticleInterest` 실행
                2. 기사마다 sourceUrl로 기존 저장 여부 확인
                    1. 기존 기사 있으면 해당 엔티티 재사용
                    2. 기사가 있지만 삭제 상태 - 건너뜀
                    3. 기사가 없으면 ArticleService.save(article)로 저장
                3. 기사와 관심사 연결
                    1. `ArticleInterestService.getOrCreateArticleInterest(article, interest)`
                    2. 현 트랜잭션에 참여
                4. 최종적으로 저장한 기사 수 반환
            4. 커밋

## 알림

1. 알림 리스너가 별도의 스레드에서 실행되고 있음
2. 한 관심사에 대한 기사 수집이 완료되면 `onArticlesSaved(event)` 실행됨
3. 알림 생성 트랜잭션 시작: `NotificationApplication.createArticleNotifications(interestId, InterestName, count)`
    * 내부적으로:
        * 구독자 ID 조회: `SubscriptionService.findSubscriberIds(interestId)`
        * 활성 사용자 조회: `UserService.getActiveUsers(subscriberIds)`
        * 실제 생성: `NotificationService.createArticleNotifications(...)`
4. 알림 생성 트랜잭션 종료
