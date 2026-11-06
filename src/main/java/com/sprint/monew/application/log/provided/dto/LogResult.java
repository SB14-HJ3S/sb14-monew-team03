package com.sprint.monew.application.log.provided.dto;

import java.nio.file.Path;
import java.util.List;

public record LogResult(List<Path> logPath) {
    public static LogResult from(List<Path> logPath) {
        return new LogResult(logPath);
    }
}
