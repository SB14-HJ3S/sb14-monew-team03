package com.sprint.monew.application.interest;

import com.sprint.monew.application.interest.provided.dto.InterestQuery;
import com.sprint.monew.domain.interest.entity.Interest;
import com.sprint.monew.global.pagination.PageResult;
import java.util.List;
import java.util.UUID;

public interface InterestService {
    Interest create(String name, List<String> keywords);

    PageResult<Interest> findPageByQuery(InterestQuery query);

    /**
     * 관심사를 구독 중인 사용자 ID 전체를 반환한다. (알림 생성용) - 페이지네이션 없이 전부 반환 - 논리 삭제된 사용자 제외 - 구독자가 없으면 빈 리스트 구현:
     * 관심사 담당 (InterestServiceImpl)
     */
    default List<UUID> findSubscriberIds(UUID interestId) {
        throw new UnsupportedOperationException("TODO: 관심사 담당 구현 예정");
    }
}
