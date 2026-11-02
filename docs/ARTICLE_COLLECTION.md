# 기사 수집, 알림 생성 메커니즘 정리

## 전체 흐름

```text
매시간 정각 — Asia/Seoul
※ 스케줄링 활성화 시
    ↓
ArticleCollectionScheduler.collect()
    ↓
ArticleCollectionRunner.runCollection()
    └─ 현재 실행 코드 없음 -> 나중에 Batch Job 연결 예정


현재 ArticleCollectionApplication에서 제공하는 기능
    │
    ├─ findValidInterestKeywords()
    │   ├─ 구독자가 있는 관심사와 키워드 조회
    │   └─ 관심사·키워드 쌍 목록 반환
    │
    ├─ collectRssArticles(source)
    │   ├─ 전달받은 언론사 하나의 RSS 수집
    │   ├─ ROME 파싱 → CollectedArticle 변환
    │   ├─ URL 기준 중복 제거
    │   └─ 기사 목록 반환
    │
    ├─ matchesInterest(article, interest)
    │   ├─ 기사 제목·요약과 관심사 키워드 비교
    │   └─ 키워드 중 하나라도 포함되면 true
    │
    ├─ collectNaverArticles(keyword)
    │   ├─ 전달받은 키워드 하나로 네이버 검색
    │   ├─ 100건씩 최대 10페이지 요청
    │   ├─ NaverNewsMapper → CollectedArticle 변환
    │   ├─ URL 기준 중복 제거
    │   └─ 기사 목록 반환
    │
    └─ saveArticles(interest, articles)
        ┌─ @Transactional — REQUIRED
        │  기존 트랜잭션이 있으면 참여, 없으면 새로 시작
        │
        ├─ 기사별 URL 조회
        ├─ 신규 기사 저장 / 기존 기사 재사용
        ├─ 삭제된 기사는 건너뜀
        ├─ 기사·관심사 연결 조회·생성
        └─ 신규 저장 기사 수 반환
            ※ 바깥 chunk 트랜잭션이 있으면 최종 커밋은 Batch에서 수행


알림 처리
    │
    ├─ ArticlesSavedEvent 발행
    │   └─ 현재 호출 코드 없음 — Batch에서 연결 예정
    │
    └─ 트랜잭션 안에서 이벤트 발행 후 커밋되면
        ↓
        NotificationEventListener.onArticlesSaved(event)
        └─ AFTER_COMMIT + @Async
            ↓
            별도 스레드에서 알림 생성
            ↓
            NotificationApplication.createArticleNotifications(...)
            ┌─ REQUIRES_NEW 트랜잭션 시작
            ├─ 관심사 구독자 조회
            ├─ 활성 사용자 조회
            ├─ 알림 생성·저장
            └─ 커밋
```

## 기사 수집

1. 정각에 스케줄러가 실행되어 Runner를 호출한다.
    - `ArticleCollectionScheduler.collect()`
    - `ArticleCollectionRunner.runCollection()`
    - **현재 Runner의 실행 코드가 비어 있어 실제 수집은 시작되지 않는다.**
    - 기존 `collectArticles()`는 제거됐으며, Batch Job 연결이 필요하다.

2. 수집할 관심사·키워드를 조회한다.
    - `ArticleCollectionUseCase.findValidInterestKeywords()` 호출
    - 내부적으로 `InterestService.findSubscribedWithKeywords()` 실행
    - 구독자가 있고 키워드를 가진 관심사를 조회한다.
    - 관심사의 키워드를 하나씩 펼쳐 `List<InterestKeywordResult>`로 반환한다.
    - 각 항목에는 `Interest` 엔티티와 키워드 하나가 들어 있다.
    - `@Transactional(readOnly = true)`가 적용된다.

3. RSS 기사를 수집한다. **선수집, 후선별**
    1. `collectRssArticles(source)` 호출
        - `HANKYUNG`, `CHOSUN`, `YEONHAP` 중 전달받은 출처 하나를 수집한다.
        - 출처별 반복 호출과 결과 보관은 Batch에서 담당해야 한다.
    2. 내부적으로 `RssNewsAdapter`가 다음을 수행한다.
        - RestClient로 RSS 요청
        - `XmlReader`, `SyndFeedInput`으로 XML 파싱
        - `RssNewsMapper`로 `CollectedArticle` 변환
        - URL·제목·발행일 검증 및 HTML 정제
    3. 이번 호출 결과를 URL 기준으로 중복 제거한다.
    4. `List<CollectedArticle>`을 반환한다.
        - 이 단계에서는 관심사 매칭이나 DB 저장을 수행하지 않는다.

4. RSS 기사와 관심사를 매칭한다.
    1. `matchesInterest(article, interest)` 호출
    2. 기사 제목과 요약을 합쳐 소문자로 변환한다.
    3. 관심사 키워드의 앞뒤 공백을 제거하고 소문자로 변환한다.
    4. 키워드 중 하나라도 포함되면 `true`, 아니면 `false`를 반환한다.

    - Batch에서 이 메서드를 이용해 관심사별 RSS 기사 목록을 선별해야 한다.

5. 네이버 기사를 수집한다. **키워드 검색으로 수집**
    1. `collectNaverArticles(keyword)` 호출
        - 관심사 전체가 아닌 키워드 하나를 전달한다.
    2. `NaverNewsPort.search(keyword)` 실행
    3. 내부적으로 `NaverNewsAdapter`가 100건씩 최대 10페이지를 요청한다.
    4. `NaverNewsMapper`가 결과를 `CollectedArticle`로 변환한다.
    5. 이번 호출 결과를 URL 기준으로 중복 제거한다.
    6. `List<CollectedArticle>`을 반환한다.

    - 여러 관심사·키워드의 반복 처리는 Batch에서 담당해야 한다.
    - 여러 호출의 결과를 합친 뒤 중복을 제거하는 작업도 별도로 필요하다.

6. 기사를 저장하고 관심사에 연결한다.
    1. `saveArticles(interest, articles)` 호출
    2. `@Transactional`의 기본 `REQUIRED` 적용
        - 기존 트랜잭션이 있으면 참여한다.
        - 없으면 새 트랜잭션을 시작한다.
        - 기존의 저장용 `TransactionTemplate`과 `REQUIRES_NEW`는 사용하지 않는다.
    3. 기사마다 `sourceUrl`로 기존 저장 여부를 확인한다.
        - 기존 기사가 있으면 해당 엔티티를 재사용한다.
        - 기존 기사가 삭제 상태이면 건너뛴다.
        - 기존 기사가 없으면 `ArticleService.save(article)`로 저장한다.
    4. 기사와 관심사를 연결한다.
        - `ArticleInterestService.getOrCreateArticleInterest(article, interest)` 호출
        - 현재 트랜잭션에 참여하며, 연결이 없을 때만 생성한다.
    5. 신규 저장 기사 수를 반환한다.
        - 기존 기사에 관심사 연결만 추가한 경우는 신규 기사 수에 포함하지 않는다.
        - 저장 예외는 잡지 않고 호출자에게 전달한다.
    6. 트랜잭션을 커밋한다.
        - Batch의 chunk 트랜잭션에 참여했다면 최종 커밋은 Batch가 수행한다.
        - **메서드 반환 시점과 chunk 커밋 완료 시점은 다를 수 있다.**
        - 성공 메트릭은 커밋 이후 반영해야 한다.

## 알림

1. 현재 기사 수집 코드에서는 `ArticlesSavedEvent`를 발행하지 않는다.
    - 기존 발행 코드는 참고용 주석으로만 남아 있다.
    - Batch에서 커밋된 신규 기사 수를 집계하고 이벤트 발행을 연결해야 한다.
    - 관심사별 알림 한 번을 유지하려면 해당 관심사의 수집 결과를 합산해야 한다.

2. 이벤트는 트랜잭션 안에서 발행해야 한다.
    - 리스너에 `@TransactionalEventListener(AFTER_COMMIT)`이 적용되어 있다.
    - 이벤트가 발행된 트랜잭션이 커밋된 뒤 리스너가 실행된다.
    - 트랜잭션 밖에서 발행하면 현재 설정에서는 해당 리스너가 실행되지 않는다.

3. `NotificationEventListener.onArticlesSaved(event)`가 실행된다.
    - `@Async`가 적용되어 별도 스레드에서 처리한다.
    - `NotificationApplication.createArticleNotifications(interestId, interestName, count)`를 호출한다.

4. 알림 생성 트랜잭션을 시작한다.
    - `@Transactional(propagation = REQUIRES_NEW)` 적용
    - 기사 저장과 독립적인 트랜잭션으로 처리한다.
    - 내부 처리:
        - 구독자 ID 조회: `SubscriptionService.findSubscriberIds(interestId)`
        - 활성 사용자 조회: `UserService.getActiveUsers(subscriberIds)`
        - 알림 생성·저장: `NotificationService.createArticleNotifications(...)`

5. 알림 생성 트랜잭션을 종료한다.
    - 성공하면 커밋한다.
    - 실패하면 롤백하고 리스너에서 오류 로그를 남긴다.
    - 이미 커밋된 기사 저장은 취소되지 않는다.
    - 비동기 알림 생성 실패는 기사 수집 호출자에게 전달되지 않는다.
