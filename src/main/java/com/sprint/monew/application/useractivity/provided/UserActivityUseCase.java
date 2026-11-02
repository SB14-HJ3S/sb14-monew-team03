package com.sprint.monew.application.useractivity.provided;

import com.sprint.monew.application.useractivity.provided.dto.UserActivityResult;
import java.util.UUID;

public interface UserActivityUseCase {
    /** 사용자의 활동 내역을 조회한다. 없거나 탈퇴한 사용자면 404. */
    UserActivityResult findByUserId(UUID userId);
}
