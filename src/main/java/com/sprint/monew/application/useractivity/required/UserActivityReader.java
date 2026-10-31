package com.sprint.monew.application.useractivity.required;

import com.sprint.monew.application.useractivity.provided.dto.UserActivityResult;
import java.util.Optional;
import java.util.UUID;

public interface UserActivityReader {
    /** 활성 사용자의 활동 내역. 없거나 탈퇴한 사용자면 빈 값. */
    Optional<UserActivityResult> findByUserId(UUID userId);
}
