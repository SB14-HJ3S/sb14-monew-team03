package com.sprint.monew.application.log.provided;

import com.sprint.monew.application.log.provided.dto.LogBackupResult;
import java.io.IOException;
import java.time.LocalDate;

public interface LogBackupUseCase {

    LogBackupResult backup(LocalDate targetDate) throws IOException;
}
