package com.sprint.monew.application.log.required;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

public interface LogStoragePort {

    boolean upload(LocalDate targetDate, long partNumber, List<Path> files);
}
