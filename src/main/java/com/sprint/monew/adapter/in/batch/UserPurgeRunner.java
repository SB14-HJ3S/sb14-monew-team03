package com.sprint.monew.adapter.in.batch;

import com.sprint.monew.application.user.provided.UserUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionException;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserPurgeRunner {

    private final UserUseCase userUseCase;

    public void runPurge() {
        try {
            userUseCase.purgeDeletedUsers();
        } catch (DataAccessException | TransactionException e) {
            log.error("사용자 완전 삭제 배치 실패: DB 오류", e);
        } catch (RuntimeException e) {
            log.error("사용자 완전 삭제 배치 중 예상하지 못한 오류", e);
        }
    }
}
