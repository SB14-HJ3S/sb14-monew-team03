# codecov-badge

README 상단의 CI·커버리지 배지 그림만 두는 전용 브랜치입니다. **머지하지 않고, 지우지 마세요.**

- `ci.yml`의 `badge` job이 develop CI가 끝날 때마다 `ci.svg`, `codecov.svg`를 덮어씁니다.
- develop·main은 Ruleset으로 직접 push가 막혀 있어서, 배지 갱신 커밋을 develop 대신 이 브랜치에 둡니다 (#329 참고).
