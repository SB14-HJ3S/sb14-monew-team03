# 복구 로직

## 복구 기사의 재백업 제외

- 복구는 `Article.restore()`로 기사를 생성하고 `restoredAt`에 복구 시각을 기록한다.
- 기사 ID와 `createdAt`은 새로 생성되며, `publishedAt`은 백업 값을 유지한다.
- 일반 수집의 `Article.create()`는 `restoredAt`을 `null`로 유지한다.
- 정기·로컬 수동 백업의 공통 조회 조건은 `restoredAt IS NULL`이다. 복구 기사는 이후 날짜에도 백업하지 않는다.
- 복구 후 관심사 연결 등이 변경되어도 재백업하지 않으므로, 기존 S3 백업에는 그 변경이 반영되지 않는다.
- API 응답 DTO와 S3 JSON 형식은 변경하지 않는다. 기존 백업 파일도 그대로 복구할 수 있다.

## DB 적용

- 새 코드 배포 전에 `scripts/sql/20261107_add_article_restored_at.sql`을 대상 DB에 적용한다.
- nullable 컬럼 추가이므로 기존 행은 `null`을 유지한다. 구버전 코드가 생성하는 기사도 `null`이다.
- 저장소 기본 설정은 `ddl-auto: update`이지만 배포 시 자동 DDL에 의존하지 않고 컬럼을 먼저 준비한다.
- 변경 전에 복구된 기사는 자동 식별할 수 없다. 근거가 있는 별도 보정 전에는 기존 기사와 동일하게 백업 대상이다.
- 롤백 시에는 컬럼을 남겨 두고 이전 코드를 배포할 수 있다. 다만 이전 코드는 복구 기사도 백업한다.

```text
GET /api/articles/restore
|
+-- ArticleRestoreApplication.restoreArticles()
    @Transactional 없음
    |
    +-- 날짜 검증
    |
    +-- 날짜별 반복
    |
    +-- S3 파일 목록 조회             [TX 없음]
        |
        +-- 파일별 반복
            |
            +-- S3 파일 다운로드         [TX 없음]
            +-- Gzip 압축 해제            [TX 없음]
            +-- JSON -> 기사 목록         [TX 없음]
            +-- 요청 발행일 범위 필터링   [TX 없음]
            |
            +-- ArticleRestoreService.restore()
            |    @Transactional
            |
            +-- [TX R1 시작]
            |    +-- 현재 존재하는 관심사 조회
            |    +-- 기존 기사 URL 조회
            |    +-- 복구 대상 선정
            |    +-- 기사 saveAll()       -> R1에 참여
            |    +-- 관심사 연결 saveAll() -> R1에 참여
            +-- [TX R1 커밋]
            |
            +-- 다음 S3 파일 읽기         [TX 없음]
            +-- 다음 파일 DB 처리         [새 TX R2]
```
