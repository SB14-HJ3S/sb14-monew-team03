package com.sprint.monew.application.useractivity.required;

import com.sprint.monew.application.useractivity.provided.dto.UserActivityResult;
import java.util.Optional;
import java.util.UUID;

/**
 * 활동 내역 조회 포트 (MongoDB 문서). 문서가 없으면 빈 값을 돌려준다. 탈퇴(논리 삭제) 중에도 문서가 남아 있으므로 탈퇴 여부는 확인하지 않는다. 호출하는 쪽에서
 * 활성 사용자인지 먼저 확인해야 한다.
 */
public interface UserActivityReader {

    Optional<UserActivityResult> findByUserId(UUID userId);
}
