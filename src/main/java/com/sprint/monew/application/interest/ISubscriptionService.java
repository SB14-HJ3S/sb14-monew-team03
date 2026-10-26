package com.sprint.monew.application.interest;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface ISubscriptionService {
    Set<UUID> findSubscribedInterestIds(UUID requestId, List<UUID> interestIds);

    /**
     * 관심사를 구독 중인 사용자 ID 전체를 반환한다. (알림 생성용) - 페이지네이션 없이 전부 반환 - 논리 삭제된 사용자 제외 - 구독자가 없으면 빈 리스트 구현:
     * 관심사 담당 (InterestServiceImpl)
     */
    List<UUID> findSubscriberIds(UUID interestId);
}
