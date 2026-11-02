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
      ├─ ① UserHardDeletingEvent(userId) 발행
      │   └─ @EventListener — 같은 트랜잭션, 사용자 삭제 전에 실행
      │       └─ InterestEventListener.onUserDeleted(event)
      │           ├─ IUserService.validateDeactivatedUser(userId)
      │           ├─ 그 사용자가 구독한 관심사 ID 조회
      │           └─ 관심사 구독자 수 1씩 감소
      │
      └─ ② repository.deleteById(userId)
          └─ DELETE FROM users → DB의 ON DELETE CASCADE가 연관 행 삭제
  ↓
커밋
  ↓
204 No Content
```

## 연관 데이터 처리

> (추후 수정 예정) 표시된 항목은 각 도메인 담당자 코드가 반영되면 내용을 수정할 예정이다

| 데이터 | 처리 방식 | 처리 위치 |
| --- | --- | --- |
| 작성한 댓글 (`comments`) | 같이 삭제 | DB CASCADE (`Comment.user`) |
| 작성한 댓글에 달린 좋아요 | 같이 삭제 | DB CASCADE (`CommentLike.comment`) |
| 구독 (`subscriptions`) | 같이 삭제 | DB CASCADE |
| 알림 (`notifications`) | 같이 삭제 | DB CASCADE |
| 기사 조회 기록 (`article_view_statuses`) | 같이 삭제 | DB CASCADE |
| 관심사 구독자 수 (`interests.subscriber_count`) | 이벤트로 감소 | 관심사 도메인 리스너 (삭제 전, 같은 트랜잭션) |
| 누른 댓글 좋아요 (`comment_likes`) | 같이 삭제 (추후 수정 예정) | 댓글 도메인 (추후 수정 예정) |
| 댓글 좋아요 수 · 기사 댓글 수 | 이벤트로 감소 (추후 수정 예정) | 댓글 도메인 리스너 (추후 수정 예정) |
| MongoDB 활동 내역 | 이벤트로 삭제 (추후 수정 예정) | 활동 내역 도메인 리스너 (추후 수정 예정) |

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
    1. `UserHardDeletingEvent(userId)` 발행 — **반드시 삭제보다 먼저**
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

## 다른 도메인에서 물리 삭제에 맞춰 처리할 때

1. `UserHardDeletingEvent`(`application/user/event`)를 받는 리스너를 만든다. `event.userId()`로 지울 사용자 ID를 꺼낸다
2. 받는 시점을 고른다
    * 사용자 행이 지워지기 전에 DB 값(카운트 등)을 고쳐야 하면 `@EventListener` — 같은 트랜잭션이라 실패하면 사용자 삭제도 같이 취소된다
    * 삭제가 확정된 뒤에 처리해야 하면(MongoDB 등 다른 저장소) `@TransactionalEventListener(phase = AFTER_COMMIT)`
3. 받는 쪽에서 "탈퇴한 사용자인지" 다시 확인하려면 `IUserService.validateDeactivatedUser(userId)`를 부른다. 탈퇴하지 않았거나 없으면 404 `USER_NOT_FOUND`를 던진다
4. 사용자 쪽 코드는 고칠 필요가 없다. 이벤트는 이미 발행된다

## 참고

* 물리 삭제는 화면(프론트)에서 호출하지 않는다. 테스트로만 확인한다
    * `UserServiceTest`, `UserControllerTest`, `UserRepositoryAdapterTest`, `UserHardDeleteIntegrationTest`
* 운영 DB가 `ddl-auto: update`로 예전에 만들어졌으면 외래키에 `ON DELETE CASCADE`가 없을 수 있다. 그러면 물리 삭제가 외래키 위반으로 실패한다
