---
name: "🚀 Deploy"
about: 배포 관련 작업
title: "[Deploy] : "
labels: deploy:배포
assignees: ''
---

## 🌿 Branch Keyword
<!-- 영어 키워드(소문자)를 적으면 이슈 등록과 동시에 {타입}/{이슈번호}-{키워드} 브랜치가 자동으로 생깁니다. (추천)
     deploy 라벨은 chore 타입이라 chore/로 시작해요. 브랜치를 직접 만들 땐 비워 두되, 이름은 같은 규칙으로 만들어 주세요.

     예: 이슈 #110, 라벨 deploy:배포, 키워드 login-api → chore/110-login-api 브랜치 자동 생성 -->

## 💡 추천 브랜치 키워드
<!-- 이슈를 대신 만들어 줄 때 추천 키워드를 적어 두는 칸이에요. (직접 만드는 이슈면 비워 두세요)
     담당자는 이 키워드를 복사해서 위 🌿 Branch Keyword 칸에 붙여 넣으면 브랜치가 자동으로 생겨요. -->

## 📄 개요
배포 작업 내용을 한 문장으로 작성해주세요.

## 🌐 배포 환경
- [ ] local (Docker Compose)
- [ ] production (AWS ECS · EC2)

## ✅ 작업 범위
- [ ] 작업 1
- [ ] 작업 2

## ☑️ 배포 전 체크리스트
- [ ] 환경변수 확인 (로컬 .env / AWS SSM Parameter Store·ECS 작업 정의)
- [ ] 관련 PR 모두 develop → main 머지 완료

## 🔙 롤백 계획
배포 실패 시 롤백 방법을 작성해주세요.
<!-- 기본: 배포 실패 시 ECS가 마지막 성공 버전으로 자동 롤백 -->

## 🔗 참고 사항
- 관련 PR:
