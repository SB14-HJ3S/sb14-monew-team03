# 복구 로직

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
