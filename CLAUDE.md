# MoNew 백엔드 프로젝트

> ⚠️ **임시본입니다.** 팀 컨벤션은 팀 미팅에서 확정된 내용("확정된 것")을 반영했고, 패키지 구조(헥사고날 아키텍처)가 확정되면 최종본으로 보완할 예정입니다.

이 파일은 Claude Code가 이 레포에서 코드 리뷰·작업을 할 때 참고하는 가이드입니다.
전체 기능/API 명세/ERD 등 상세 요구사항은 코드잇이 제공한 노션 안내 문서(미션 안내 페이지)와 Swagger를 참고한다.

**모든 리뷰·코멘트·설명은 한국어로 작성한다.** (대화형 세션, `@claude` 멘션, PR 자동 리뷰 전부) 코드 식별자는 원문 그대로 두되, 그 외 문장에 영어를 섞지 않는다.

---

## 프로젝트 개요

**MoNew(모뉴)**는 네이버 뉴스 API와 언론사 RSS를 모아 관심사 기반으로 뉴스를 제공하는 뉴스 큐레이션 백엔드 API입니다. 관심사 구독, 뉴스 수집·백업·복구, 댓글·좋아요, 알림, 활동 내역 기능을 제공하며, 코드잇에서 제공하는 고정 프론트엔드와 Swagger 명세 기준으로 연동합니다.

- 난이도: 중급 (스프린트 백엔드 트랙 팀 프로젝트)
- 프론트엔드: `src/main/resources/static/`은 코드잇이 준 프론트 빌드 결과물이라 리뷰하지 않는다
- 패키지: 공통 코드는 `global/`, 도메인 코드는 `domain/{도메인}` (헥사고날 아키텍처로 구성 예정)

## 기술 스택

- **Framework**: Java 17, Spring Boot 3.5.16, Gradle(Groovy)
- **Database & ORM**: PostgreSQL 16 + Spring Data JPA, MongoDB 7 `심화`(활동 내역 조회 모델)
- **Query**: QueryDSL(OpenFeign 포크 6.x)
- **Scheduling**: Spring Scheduler / Spring Batch `심화`
- **Storage**: AWS S3 (뉴스 백업·복구, 로그 적재)
- **Test**: JUnit 5, Testcontainers(PostgreSQL·MongoDB, H2 안 씀)
- **External API**: 네이버 뉴스 API, 한국경제·조선일보·연합뉴스 RSS

### 인증 방식
로그인 후 요청은 세션·토큰 없이 `Monew-Request-User-ID` 헤더로 사용자를 구분합니다(요구사항). 헤더는 Swagger에 `Monew-Request-User-ID`가 적힌 API에서만 받습니다.

## 팀 컨벤션

**확정된 것:**
- 네이밍: 변수·함수 `camelCase`, 클래스 `PascalCase`, 패키지 소문자, 테이블 `snake_case`·소문자·복수형, 컬럼 `snake_case`
- 클래스 이름: 구현체 `~Impl`, 그 외 `~Controller` / `~Service` / `~Repository`
- Lombok: `@Getter` 필수, `@Setter` 금지
- 객체 생성: 정적 팩토리 메서드, 이름은 `create`
- 로그: 로거(`log.info` 등) 사용
- MapStruct: 사용 안 함 (Entity ↔ DTO 변환은 직접 작성)
- TDD: Red-Green-Refactor 단계별 커밋 (`test(red)` → `feat(green)`/`fix(green)` → `refactor(blue)`)
- 공통 코드: `global/`은 여러 도메인이 같이 쓰는 것만

## 도메인 & 핵심 기능 (요구사항 기준)

- **사용자**: 이메일 중복 불가, 닉네임만 수정, 이메일 + 비밀번호로 로그인, 논리 삭제 후 1일 뒤 배치로 완전 삭제
- **관심사**: 80% 이상 유사한 이름 등록 불가, 키워드는 여러 개·키워드만 수정, 물리 삭제, 구독·구독 취소
  - 검색: 이름·키워드 부분일치 / 정렬: 이름·구독자 수
- **뉴스 기사**: 네이버 API + 한경·조선·연합 RSS에서 관심사 키워드를 포함한 기사만 매시간 수집, 원본 링크 중복 불가, 조회수는 같은 사용자 1회만, 논리 삭제
  - 목록: 검색(제목·요약 부분일치), 필터(관심사·출처·날짜, 모두 만족), 정렬(날짜·댓글 수·조회 수 중 1개)
  - 백업·복구: 날짜 단위로 S3에 배치 백업, 복구는 날짜 범위로 S3 백업과 DB를 비교해 유실분만 새로 등록
- **댓글**: 기사별 등록, 본인만 수정, 논리 삭제(물리 삭제는 UI 없이 테스트로만 검증), 좋아요·좋아요 취소
  - 기사별 목록 정렬: 날짜·좋아요 수 중 1개
- **활동 내역**: 사용자 정보 + 구독 중인 관심사 + 최근 작성한 댓글·좋아요 누른 댓글·본 기사 각각 최대 10건
- **알림**: 하나씩 확인·전체 확인, 미확인만 시간순 조회, 확인 후 1주 지난 알림은 매일 배치 삭제
  - 구독 관심사에 새 기사 등록 시: `[관심사]와 관련된 기사가 0건 등록되었습니다.` (리소스: 관심사)
  - 내 댓글에 좋아요 시: `[사용자]님이 나의 댓글을 좋아합니다.` (리소스: 댓글) — 두 문구 모두 그대로
- **배치**: 뉴스 수집(매시간), 뉴스 백업(날짜 단위), 사용자 완전 삭제(논리 삭제 1일 후), 알림 삭제(매일)
- **공통**: 목록 조회는 모두 커서 페이지네이션. 논리 삭제가 기본 원칙(관련 정보 유지)이고, 물리 삭제 시에는 관련 정보도 모두 삭제

### 심화 요구사항
- **MongoDB 조회 최적화**: 활동 내역 조회의 과도한 조인 문제를, 활동이 일어날 때마다 미리 갱신하는 조회용 역정규화 모델(MongoDB)로 해결
- **Spring Batch**: 배치 작업을 Spring Batch로 관리하고, 커스텀 메트릭을 Actuator로 모니터링
- **로그 S3 적재**: 날짜별 로그를 S3에 주기적으로 적재

## 기술 요구사항

- 엔티티: 모든 엔티티는 `BaseEntity`(UUID PK + `createdAt`)를 상속하고, `createdAt`은 JPA Auditing이 자동으로 채운다(직접 넣지 않음)
  - 논리 삭제 대상(사용자·뉴스기사·댓글)은 `BaseSoftDeletableEntity`를 상속한다. Enum은 `@Enumerated(EnumType.STRING)`
- 응답: 성공은 DTO 그대로(래퍼 없음), 실패는 `GlobalExceptionHandler`가 `ErrorResponse`로 통일한다. Entity를 직접 응답하지 않는다
- DTO: `record`로 만들고, 클래스명·필드명은 코드잇 Swagger 스키마 그대로 쓴다
- 커스텀 예외: 예외 클래스는 `MonewException` 1개, 에러 종류는 도메인별 `ErrorType` enum(`global/exception/errortype/`)에 상수를 추가한다
  ```java
  throw new MonewException(UserErrorType.USER_NOT_FOUND);
  throw new MonewException(UserErrorType.USER_NOT_FOUND, Map.of("userId", userId)); // 추가 정보
  throw new MonewException(ExternalErrorType.EXTERNAL_API_CALL_FAILED, e);         // 외부 예외 감싸기
  ```
  - `IllegalArgumentException`·`RuntimeException` 등을 그대로 던지면 공통 핸들러에서 500으로 나간다
  - 외부 서비스(네이버·RSS·S3) 예외는 부르는 클래스 안에서 `ExternalErrorType`으로 바꿔 던진다. 메시지에 키·버킷 이름 등 내부 정보를 넣지 않는다
- 지연 로딩: OSIV를 꺼 둬서(`open-in-view: false`) 컨트롤러·응답 변환 단계에서는 연관 엔티티를 읽을 수 없다. 필요한 데이터는 서비스 트랜잭션 안에서 DTO로 만들어 넘긴다
- 설정값: API 키·비밀번호 같은 값은 환경변수(로컬은 `.env`)로만 받는다. 코드나 yml에 실제 값을 적지 않는다
- 로그: 요청별 ID·IP를 MDC에 넣어 로그와 응답 헤더에 남긴다
- 엔티티 변경: 운영 DB는 `ddl-auto: update`라, 이미 데이터가 있는 테이블에 `nullable = false` 컬럼을 추가하거나 타입을 바꾸면 운영에서만 반영이 실패한다(CI는 빈 DB라 못 잡음). 새 컬럼은 nullable로 두거나 기본값을 준다
- 테스트: 통합 테스트는 `@IntegrationTest`, MongoDB가 필요한 테스트만 `@MongoIntegrationTest`를 쓴다
  - `@MockBean`·`@TestPropertySource`는 꼭 필요할 때만 쓴다. 설정이 다른 테스트마다 스프링 컨텍스트와 컨테이너가 새로 떠서 CI가 느려진다
  - 외부 서비스를 실제로 부르는 테스트는 `@Tag("external-api")`를 붙인다

### 요청 검증(Bean Validation) 컨벤션

1. 요청 본문 DTO는 컨트롤러 파라미터에 `@Valid @RequestBody`를 붙인다. 안 붙이면 DTO 검증이 조용히 무시된다.
2. `@RequestParam`·`@PathVariable`에 붙인 제약(`@Min` 등)은 `@Validated` 없이도 동작한다(Spring Boot 3.2 이상).
3. 검증 실패는 직접 try-catch하지 않는다. `GlobalExceptionHandler`가 400과 `details{필드: 메시지}`로 응답한다.
4. 제약에 `message`를 쓰면 한국어로 쓴다. 응답 `details`에 그대로 나간다.
5. Swagger에 있는 필수값·길이 제약은 이미 DTO에 들어가 있다. 그 밖의 규칙(이메일 형식, 비밀번호 조합 등)은 담당자가 TDD로 넣는다.
