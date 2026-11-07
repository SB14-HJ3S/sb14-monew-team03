package com.sprint.monew.application.log.provided.usecase;

import com.sprint.monew.application.log.provided.usecase.dto.LogBackupResult;
import com.sprint.monew.application.log.provided.usecase.dto.LogResult;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

public interface LogBackupUseCase {

    LogResult readLogs(
            @NotNull @Past LocalDate targetDate, @Min(-1) int afterIndex, @Min(1) int limit)
            throws IOException;

    LogBackupResult backup(
            @NotNull @Past LocalDate targetDate,
            @Min(1) long partNumber,
            @NotEmpty List<@NotNull Path> logPath);
}
