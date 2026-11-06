# 사용자 물리 삭제 흐름 정리

## 전체흐름:

```text
DELETE /api/users/{userId}/hard
  ↓
UserController.hardDeleteUser(userId)
  ↓
UserApplication.hardDeleteUser(userId)
  ↓
UserService.hardDeleteUser(userId) — @Transactional
  │
  ├─ findUserIncludingDeletedForUpdate(userId) — SELECT ... FOR UPDATE
  │   ├─ 사용자가 없음 → 404 USER_NOT_FOUND
  │   ├─ 탈퇴(논리 삭제)하지 않은 사용자 → 404 USER_NOT_FOUND
  │   └─ 탈퇴한 사용자 → 행 잠금 후 계속
  │
  └─ removeCompletely(user)
      │
      ├─ ① UserHardDeletingEvent(List.of(userId)) 발행
      │   ├─ @EventListener — 같은 트랜잭션, 사용자 삭제 전에 실행
      │   │   ├─ InterestEventListener.onUserDeleted(event)
      │   │   │   └─ cleanupSubscriberCountBeforeUserHardDeletion(event.userIds())
      │   │   │       └─ 구독한 관심사마다 지울 사용자의 구독 수만큼 한 번에 감소 (0 아래로 안 내려감)
      │   │   └─ CommentEventListener.onUserHardDeleting(event)
      │   │       └─ cleanupCountsBeforeUserHardDeletion(event.userIds())
      │   │           └─ 누른 댓글 좋아요 수 · 작성한 댓글의 기사 댓글 수 감소
      │   └─ @TransactionalEventListener(AFTER_COMMIT) — 커밋 뒤 실행
      │       └─ UserActivityEventListener.on(event)
      │           └─ usersHardDeleted(event.userIds()) — MongoDB 활동 내역 삭제
      │
      └─ ② repository.deleteById(userId)
          └─ DELETE FROM users → DB의 ON DELETE CASCADE가 연관 행 삭제
  ↓
커밋
  ↓
204 No Content
```

## 연관 데이터 처리

| 데이터 | 처리 방식 | 처리 위치 |
| --- | --- | --- |
| 작성한 댓글 (`comments`) | 같이 삭제 | DB CASCADE (`Comment.user`) |
| 작성한 댓글에 달린 좋아요 | 같이 삭제 | DB CASCADE (`CommentLike.comment`) |
| 구독 (`subscriptions`) | 같이 삭제 | DB CASCADE |
| 알림 (`notifications`) | 같이 삭제 | DB CASCADE |
| 기사 조회 기록 (`article_view_statuses`) | 같이 삭제 | DB CASCADE |
| 관심사 구독자 수 (`interests.subscriber_count`) | 이벤트로 감소 | 관심사 도메인 리스너 (삭제 전, 같은 트랜잭션) |
| 누른 댓글 좋아요 (`comment_likes`) | 같이 삭제 | DB CASCADE (`CommentLike.user`) |
| 댓글 좋아요 수 · 기사 댓글 수 | 이벤트로 감소 | 댓글 도메인 리스너 (삭제 전, 같은 트랜잭션) |
| MongoDB 활동 내역 | 이벤트로 삭제 | 활동 내역 도메인 리스너 (커밋 뒤) |

## 사용자 물리 삭제

1. 요청 처리: `UserController.hardDeleteUser(userId)`
    * 요청자 헤더(`Monew-Request-User-ID`)는 받지 않는다
        * 코드잇 제공 Swagger(api-docs)의 `DELETE /api/users/{userId}/hard`는 파라미터가 path의 `userId` 하나뿐이고, 요청자 헤더 파라미터가 없다
        * 응답 목록에는 403(사용자 삭제 권한 없음)이 있지만, 권한을 판단할 요청자 정보를 받지 않아서 403은 내지 않는다
        * 응답은 204(삭제 성공), 404(사용자 정보 없음)를 쓴다
    * `{userId}`가 UUID 형식이 아니면 컨트롤러에 들어가기 전에 400 `INVALID_REQUEST`
2. 삭제 대상 확인 **@Transactional**: `UserService.hardDeleteUser(userId)`
    1. `findUserIncludingDeletedForUpdate(userId)`로 탈퇴한 사용자까지 찾으면서 행을 잠근다
    2. `filter(User::isDeleted)`로 탈퇴한 사용자만 남긴다
        * 없거나 탈퇴하지 않은 사용자면 404 `USER_NOT_FOUND`, 아무것도 지우지 않는다
3. 완전 삭제: `removeCompletely(user)`
    1. `UserHardDeletingEvent(List.of(userId))` 발행 — **반드시 삭제보다 먼저**
        * 이벤트는 사용자 ID **목록**을 담는다. 배치가 여러 명을 한 번에 지울 때도 같은 이벤트를 쓴다
        * 리스너가 실행될 때 아직 사용자·구독·댓글 행이 DB에 있어서, 무엇을 줄일지 찾을 수 있다
    2. `repository.deleteById(userId)`로 사용자 행 삭제
        * 연관 행은 엔티티의 `@OnDelete(action = OnDeleteAction.CASCADE)`로 DB가 지운다
4. 커밋 후 204 응답

## 동시 삭제

* 같은 사용자를 동시에 두 번 물리 삭제하면, 잠금이 없을 때는 두 요청이 모두 확인을 통과해 구독자 수가 두 번 줄었다
* 그래서 처음 조회를 `SELECT ... FOR UPDATE`(`@Lock(PESSIMISTIC_WRITE)`)로 한다
    * 두 번째 요청은 첫 번째가 커밋될 때까지 기다린다
    * 첫 번째가 사용자를 지운 뒤라 조회 결과가 없어서 404로 끝나고, 구독자 수는 한 번만 준다
* `UserHardDeleteIntegrationTest`의 동시 삭제 테스트로 확인한다

## 1일 지난 탈퇴자 일괄 삭제 (배치)

```text
UserCleanupScheduler — cron: monew.user.cleanup.cron (매시 정각, Asia/Seoul)
  ↓
userCleanupJob → userCleanupStep (트랜잭션 없이 실행: ResourcelessTransactionManager)
  ↓
UserCleanupTasklet → UserUseCase.cleanupDeletedUsers()
  │
  ├─ findCleanupTargetIds(지금 - 1일) — deletedAt < 기준 시각인 사용자 ID
  │
  └─ 100명씩 나눠서 UserService.hardDeleteUsers(ids) — @Transactional, 묶음마다 따로 커밋
      ├─ findDeletedUsersForUpdate(ids) — 그중 탈퇴한 사용자만 SELECT ... FOR UPDATE
      │   └─ 한 명도 없으면 0 (이벤트·삭제 없음)
      ├─ UserHardDeletingEvent(잠근 사용자 ID 목록) 한 번 발행
      └─ deleteUsers(ids) — DELETE ... WHERE id IN (...) 한 번, 지운 수 반환
  ↓
UserCleanupStepListener — 지운 수(writeCount)를 메트릭 UserCleanupMetrics.DELETED에 더함
```

* **Step에 트랜잭션을 걸지 않는다**: Step 전체를 한 트랜잭션으로 묶으면, 묶음 하나가 실패할 때 바깥 트랜잭션이 롤백 대상으로 표시되어 한 명씩 다시 지운 것까지 모두 취소된다. 그래서 묶음마다 `hardDeleteUsers`의 `@Transactional`로 따로 커밋한다
* **묶음이 실패하면** 그 묶음만 한 명씩 `hardDeleteUser(userId)`로 다시 지운다
    * `USER_NOT_FOUND`(이미 지워짐)는 건너뛰고 지운 수에 넣지 않는다
    * 그 밖의 오류는 실패로 로그를 남기고 다음 사람을 계속 지운다. 실패한 사람은 다음 실행 때 다시 대상이 된다
* **대상 조회 자체가 실패하면** Job이 FAILED로 남고 메트릭은 늘지 않는다. 같은 파라미터로 다시 실행할 수 있다
* **기준 시각 경계**: `deletedAt < 기준 시각`이라 기준 시각과 똑같이 탈퇴한 사용자는 이번 실행에서 빠지고 다음 실행에서 지워진다

## 다른 도메인에서 물리 삭제에 맞춰 처리할 때

1. `UserHardDeletingEvent`(`application/user/event`)를 받는 리스너를 만든다. `event.userIds()`로 지울 사용자 ID 목록을 꺼낸다
2. 받는 시점을 고른다
    * 사용자 행이 지워지기 전에 DB 값(카운트 등)을 고쳐야 하면 `@EventListener` — 같은 트랜잭션이라 실패하면 사용자 삭제도 같이 취소된다
    * 삭제가 확정된 뒤에 처리해야 하면(MongoDB 등 다른 저장소) `@TransactionalEventListener(phase = AFTER_COMMIT)`
3. 이벤트에는 탈퇴 확인과 행 잠금이 끝난 사용자만 담긴다. 받는 쪽에서 다시 확인할 필요는 없다
    * 여러 명을 받으면 한 명씩 반복하지 말고 한 번에 처리한다 (관심사·댓글 리스너처럼)
4. 사용자 쪽 코드는 고칠 필요가 없다. 이벤트는 이미 발행된다

## 참고

* 물리 삭제는 화면(프론트)에서 호출하지 않는다. 테스트로만 확인한다
    * `UserServiceTest`, `UserControllerTest`, `UserRepositoryAdapterTest`, `UserHardDeleteIntegrationTest`, `CommentUserHardDeleteIntegrationTest`
* 일괄 삭제 배치 테스트
    * `UserApplicationTest`, `UserCleanupIntegrationTest`, `UserCleanupJobIntegrationTest`, `UserCleanupJobFallbackIntegrationTest`, `UserCleanupSchedulerTest`
* 운영 DB가 `ddl-auto: update`로 예전에 만들어졌으면 외래키에 `ON DELETE CASCADE`가 없을 수 있다. 그러면 물리 삭제가 외래키 위반으로 실패한다
