# 기사 S3 백업 메커니즘 정리

## 구현 상태 및 적용 기준

- 기사 백업용 Scheduler, JobConfig, JobLauncher 연결 필요
- 기사 조회, 관심사 연결 조회, 백업 DTO 생성, JSON 변환, GZIP 압축, S3 업로드 기능 구현 완료

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
