# 기사 S3 백업 메커니즘 정리

## 구현 상태 및 적용 기준

- 경로 예시: `articles/2026-11-02/articles-2026-11-02-000001.json.gz`
- 기사 백업용 Scheduler, JobConfig, JobLauncher 연결 필요 !!
- 기사 조회, 관심사 연결 조회, 백업 DTO 생성, JSON 변환, GZIP 압축, S3 업로드 기능 구현 완료

## 배치 관련

**Reader: ItemReader<ArticleResult>**

- useCase.readArticles(backupDate, afterId, 1000) 호출
- 최초 afterId = null, 이후 마지막으로 반환한 기사의 id() 사용
- 조회는 1,000개씩, 반환은 하나씩. 조회 결과가 비면 null 반환
- 한국 시간 기준 생성일 조회, 논리 삭제 제외, ID 오름차순은 기존 서비스에서 처리

**Writer: ItemWriter<ArticleResult>**

- (Step chunk 크기 1,000)
- useCase.backupChunk(backupDate, partNumber, articles) 호출
- partNumber는 1부터, 저장 성공 후 증가. 파일 개수 제한 없음
- 관심사 연결·JSON 배열 변환·GZIP 압축·S3 업로드는 기존 코드가 처리
- 저장 예외는 Batch에 그대로 전달

## 전체 흐름

```text
매일 1회 — Asia/Seoul, 오전 3시 실행
※ 스케줄링 활성화 및 배치 연결 후
    ↓
ArticleBackupScheduler (추가 예정)
    ├─ 한국 시간 기준 어제 날짜 계산
    └─ JobParameters에 backupDate 전달
        ↓
JobLauncher.run(articleBackupJob, parameters)
    ↓
ArticleBackupJobConfig (추가 예정)
    └─ chunk 크기 1,000으로 Reader와 Writer 연결
        │
        ├─ ArticleBackupReader.read()
        │   ├─ ArticleBackupUseCase.readArticles(date, lastId, 1000)
        │   │   ↓
        │   │   ArticleBackupApplication
        │   │   └─ ArticleService.getArticlesForBackup(...)
        │   │       └─ RepositoryPort → Adapter → JPA 조회
        │   ├─ 최대 1,000개를 조회해 보관
        │   ├─ 호출마다 기사 하나씩 반환
        │   └─ 더 읽을 기사가 없으면 null 반환
        │
        └─ ArticleBackupWriter.write(chunk)
            ├─ 모인 기사 최대 1,000개를 UseCase에 전달
            │   ↓
            │   ArticleBackupApplication.backupChunk(...)
            │   ├─ 기사 ID 목록 생성
            │   ├─ ArticleInterestService로 관심사 ID 일괄 조회
            │   ├─ ArticleBackupItem 목록 생성
            │   ├─ ArticleBackupFile 생성
            │   └─ ArticleBackupStorageService.save(...)
            │       ↓
            │       ArticleBackupStoragePort
            │       ↓
            │       S3ArticleBackupStorageAdapter.save(...)
            │       ├─ 저장 경로 생성
            │       ├─ Serializer: 기사 목록 → JSON 배열
            │       ├─ Compressor: JSON → GZIP
            │       ├─ S3 조건부 업로드
            │       └─ ArticleBackupResult 반환
            ├─ 저장 성공 후 파일 번호 증가
            └─ 다음 기사 묶음으로 반복

전체 기사 조회가 끝나고 마지막 묶음까지 저장되면 Step 종료
```

## 기타

- zip vs. gzip
    - zip은 여러 파일을 하나로 묶어 압축
    - gzip은 데이터 하나를 압축
    - 따라서 zip은 .json 파일들을 다 만들고 압축해야 하는 반면, gzip은 기사1000개가 모이면 바로 압축, 업로드 할 수 있다.
