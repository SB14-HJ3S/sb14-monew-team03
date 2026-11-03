package com.sprint.monew.application.interest.provided;

import java.util.List;
import java.util.UUID;

public interface UserDeletionSubscriptionCleaner {
    /** 비활성 상태를 검증하고 잠금 조회한 사용자들의 물리 삭제 전에 같은 트랜잭션에서 호출한다. */
    void cleanupSubscriberCountBeforeUserHardDeletion(List<UUID> userIds);
}
