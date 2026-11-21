package com.sprint.monew.adapter.in.batch.log;

import java.time.LocalDate;

public class LogBackupFailedException extends RuntimeException {
    private final LocalDate targetDate;
    private final int totalFileCount;
    private final int uploadedFileCount;

    public LogBackupFailedException(
            LocalDate targetDate, int totalFileCount, int uploadedFileCount) {
        super(
                "로그 S3 백업 실패: targetDate=%s totalFiles=%d, uploadedFiles=%d"
                        .formatted(targetDate, totalFileCount, uploadedFileCount));
        this.targetDate = targetDate;
        this.totalFileCount = totalFileCount;
        this.uploadedFileCount = uploadedFileCount;
    }
}
