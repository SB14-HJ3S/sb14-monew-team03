package com.sprint.monew.application.log;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sprint.monew.application.log.provided.dto.LogBackupResult;
import com.sprint.monew.application.log.required.LogStoragePort;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LogBackupApplicationTest {

    @Mock private LogStoragePort logStoragePort;
    @TempDir Path logDirectory;

    @Test
    @DisplayName("백업 대상 파일 수와 저장소에서 업로드 성공이 확인된 파일 수를 구분해 반환한다")
    void countOnlyConfirmedUploads() throws Exception {
        LocalDate targetDate = LocalDate.of(2026, 10, 31);
        Path first = Files.createFile(logDirectory.resolve("monew.log.2026-10-31.0.log"));
        Path second = Files.createFile(logDirectory.resolve("monew.log.2026-10-31.1.log"));
        Path third = Files.createFile(logDirectory.resolve("monew.log.2026-10-31.2.log"));
        Files.createFile(logDirectory.resolve("monew.log"));
        Files.createFile(logDirectory.resolve("monew.log.2026-11-01.0.log"));
        LogBackupApplication application =
                new LogBackupApplication(
                        logStoragePort, logDirectory.resolve("monew.log").toString());
        when(logStoragePort.upload(first)).thenReturn(true);
        when(logStoragePort.upload(second)).thenReturn(false);
        when(logStoragePort.upload(third)).thenReturn(true);

        LogBackupResult result = application.backup(targetDate);

        assertThat(result.totalFileCount()).isEqualTo(3);
        assertThat(result.uploadedFileCount()).isEqualTo(2);
        verify(logStoragePort).upload(third);
    }

    @Test
    @DisplayName("백업 대상 파일이 없으면 총 파일 수와 성공 파일 수가 모두 0이다")
    void returnZeroCountsWhenNoFilesMatch() throws Exception {
        LogBackupApplication application =
                new LogBackupApplication(
                        logStoragePort, logDirectory.resolve("monew.log").toString());

        LogBackupResult result = application.backup(LocalDate.of(2026, 10, 31));

        assertThat(result.totalFileCount()).isZero();
        assertThat(result.uploadedFileCount()).isZero();
        verifyNoInteractions(logStoragePort);
    }

    @Test
    @DisplayName("예상하지 못한 오류는 호출자에게 전달한다")
    void propagateUploadFailure() throws Exception {
        Path file = Files.createFile(logDirectory.resolve("monew.log.2026-10-31.0.log"));
        LogBackupApplication application =
                new LogBackupApplication(
                        logStoragePort, logDirectory.resolve("monew.log").toString());
        IllegalStateException failure = new IllegalStateException("upload failed");
        when(logStoragePort.upload(file)).thenThrow(failure);

        assertThatThrownBy(() -> application.backup(LocalDate.of(2026, 10, 31))).isSameAs(failure);
    }
}
