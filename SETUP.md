## 팀원 시작 가이드 (처음 세팅하는 법)

### 0. 준비물
- **JDK 17** — 없으면 빌드가 안 돼요. IntelliJ `File > Project Structure > SDK`에서 17을 내려받거나, 터미널에서 `java -version`으로 17인지 확인하세요
- **Docker Desktop** — 로컬 DB와 테스트(Testcontainers)에 필요해요. 켜져 있어야 해요

### 1. 저장소 클론받기
```bash
git clone git@github.com:SB14-HJ3S/sb14-monew-team03.git
```
기본 브랜치가 `develop`이라 클론받으면 자동으로 `develop` 브랜치로 내려받아져요.
혹시 다른 브랜치로 열려있다면:
```bash
git checkout develop
```

### 2. IntelliJ에서 열기
클론받은 폴더를 IntelliJ로 열면(`File > Open`) `build.gradle`을 자동으로 인식해서
Gradle sync가 시작돼요. 이때 커밋 훅(`installGitHooks`)이랑 코드 스타일(`installIntellijCodeStyle`)도
자동으로 같이 깔려요 — 별도로 버튼 누를 필요 없어요.

### 3. 환경 변수 파일(.env) 만들기
`.env.example` 파일을 복사해서 `.env` 파일을 만드세요.
```bash
cp .env.example .env
```
값은 팀장이 별도로 공지할 예정이에요. (⚠️ 이 값들은 절대 코드에 직접 적거나 커밋하지 마세요!)

- `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` / `AWS_S3_BUCKET`: 로컬에서 S3 기능(뉴스기사 백업·복구, 로그 적재)을 테스트할 때만 필요해요. 로컬 전용 버킷이라 운영 데이터에는 영향이 없어요. S3 기능을 안 건드리면 비워 둬도 앱은 떠요
- `NAVER_API_CLIENT_ID` / `NAVER_API_CLIENT_SECRET`: 뉴스기사 수집용 네이버 API 키

> 참고: 로컬 Postgres/MongoDB 비밀번호는 `.env`가 아니라 `compose.yaml`에 이미 고정값(`secret`)으로 들어있어서 따로 채울 필요 없어요.

### 4. 로컬 DB 띄우기 (Postgres + MongoDB)
Docker Desktop이 켜져 있으면(메뉴바에 고래 아이콘) 앱을 실행하기만 해도 Postgres + MongoDB가 같이 떠요.
`docker compose up`을 따로 칠 필요 없어요.

### 5. 애플리케이션 실행하고 확인하기
IntelliJ에서 메인 애플리케이션 클래스(▶️ 실행 버튼)를 눌러 서버를 켜주세요.
정상적으로 실행됐다면 브라우저에서 아래 주소로 확인할 수 있어요:
- 서버 상태 확인: http://localhost:8080/actuator/health → `{"status":"UP"}` 이라고 뜨면 정상!
- Swagger(API 문서): http://localhost:8080/swagger-ui/index.html

### 6. 저장 시 자동 포맷팅 설정 (한 번만, 수동)
`installIntellijCodeStyle`(들여쓰기 스킴)까지는 2번에서 이미 자동으로 끝났지만, **저장할 때마다
Java 파일을 자동으로 리포맷 + import 정리해주는 설정은 자동 체인에 안 들어있어서 직접 한 번
실행해야 해요.**

⚠️ **반드시 IntelliJ 프로젝트를 완전히 닫은 상태에서** 터미널(또는 다른 IDE)에서 실행하세요:
```bash
./gradlew installIntellijActionsOnSave
```
IntelliJ가 켜진 채로 실행하면 IDE가 설정 파일(`.idea/workspace.xml`)을 다시 덮어써서 방금 넣은
설정이 사라질 수 있어요. 실행 후 IntelliJ를 다시 열면 적용된 상태예요.

이 설정은 편의용이에요 — 안 해도 커밋할 때 Spotless가 최종적으로 코드를 정리해주니 결과물엔
차이 없어요. 다만 저장할 때마다 바로 정리된 모습을 보고 싶으면 이 단계를 하시면 돼요.

### 7. 테스트 커버리지 확인 (선택)

```bash
./gradlew test                                   # 테스트 실행 + 커버리지 리포트 생성
open build/reports/jacoco/test/html/index.html   # 브라우저로 보기 (Windows는 start)
```

- 패키지 → 클래스 순으로 들어가면 테스트가 지나간 줄(초록)과 안 지나간 줄(빨강)이 보입니다.
- 측정에서 빠지는 것: DTO(`dto/`), 설정(`config/`), 메인 클래스, QueryDSL Q클래스, 공통 코드(`global/`). 커버리지는 각자 도메인 코드만으로 계산됩니다.
- 기준은 줄(LINE) 기준입니다. 지금은 0%라 빌드를 막지 않고, 팀 커버리지가 80%를 넘어 안정되면 그때 잠급니다.
- PR에서는 Codecov 댓글로 바뀐 코드의 커버리지를 확인할 수 있습니다.

---

여기까지 잘 되면 세팅 완료! 바로 작업 시작하시면 됩니다 🙌

### 작업 시작 순서 (꼭 지켜 주세요)

1. **이슈부터 만들기** — 이슈 게시판에서 템플릿을 골라 작성해요. Projects 보드와 일정 관리가 전부 이슈 기준이에요.
2. **브랜치 만들기** — 이름은 반드시 `{타입}/{이슈번호}-{이름}` (예: `feat/110-login-api`, 가운데 숫자가 이슈 번호)
   - 이슈의 **Branch Keyword** 칸에 영어 키워드(`login-api`)를 적으면 이 이름으로 브랜치가 자동으로 생겨요 (추천)
   - 직접 만들 때도 이름 규칙은 같아요
3. **그 브랜치에서 커밋하기** — 커밋 메시지 끝 `[#110]`이 브랜치 이름에서 자동으로 붙어요

브랜치 이름이 규칙과 다르면 커밋에 이슈 번호가 안 붙고, 일정 관리의 시작일도 안 잡혀요.
자세한 내용은 [커밋 이슈번호 자동화 안내](https://app.notion.com/p/3f99e310974381709046cd9a0c771a62) 참고.
