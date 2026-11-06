package com.sprint.monew.application.log;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.sprint.monew.application.log.required.LogStoragePort;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LogBackupApplicationTest {

    @Mock private LogStoragePort logStoragePort;
    @TempDir Path logDirectory;

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("조회한 파일 묶음을 한 번 업로드하고 성공 여부에 따라 전체 또는 0건을 반환한다")
    void uploadOneArchiveForProvidedFiles(boolean uploaded) throws Exception {
        LocalDate date = LocalDate.of(2026, 10, 31);
        List<Path> files =
                List.of(
                        Files.createFile(logDirectory.resolve("monew.log.2026-10-31.0.log")),
                        Files.createFile(logDirectory.resolve("monew.log.2026-10-31.2.log")),
                        Files.createFile(logDirectory.resolve("monew.log.2026-10-31.10.log")));
        Files.createFile(logDirectory.resolve("monew.log"));
        Files.createFile(logDirectory.resolve("monew.log.2026-11-01.0.log"));
        LogBackupApplication application = application();
        when(logStoragePort.upload(date, 2L, files)).thenReturn(uploaded);

        List<Path> readFiles = application.readLogs(date, -1, 10).logPath();
        var result = application.backup(date, 2L, readFiles);

        assertThat(readFiles).containsExactlyElementsOf(files);
        assertThat(result.totalFileCount()).isEqualTo(3);
        assertThat(result.uploadedFileCount()).isEqualTo(uploaded ? 3 : 0);
        verify(logStoragePort).upload(date, 2L, files);
        verifyNoMoreInteractions(logStoragePort);
    }

    @Test
    @DisplayName("마지막 롤오버 번호 이후의 파일만 숫자순으로 제한 조회한다")
    void readOnlyNextPage() throws Exception {
        Path first = Files.createFile(logDirectory.resolve("monew.log.2026-10-31.0.log"));
        Path second = Files.createFile(logDirectory.resolve("monew.log.2026-10-31.2.log"));
        Path third = Files.createFile(logDirectory.resolve("monew.log.2026-10-31.10.log"));
        var application = application();
        var date = LocalDate.of(2026, 10, 31);

        assertThat(application.readLogs(date, -1, 2).logPath()).containsExactly(first, second);
        assertThat(application.readLogs(date, 2, 2).logPath()).containsExactly(third);
        assertThat(application.readLogs(date, 10, 2).logPath()).isEmpty();
        verifyNoInteractions(logStoragePort);
    }

    @Test
    @DisplayName("대상 파일이 없으면 빈 목록을 반환하고 저장소를 호출하지 않는다")
    void readEmptyLogFiles() throws Exception {
        var application = application();
        var date = LocalDate.of(2026, 10, 31);
        var files = application.readLogs(date, -1, 10).logPath();

        assertThat(files).isEmpty();
        verifyNoInteractions(logStoragePort);
    }

    @Test
    @DisplayName("예상하지 못한 오류는 호출자에게 전달한다")
    void propagateUnexpectedFailure() {
        var date = LocalDate.of(2026, 10, 31);
        List<Path> files = List.of(logDirectory.resolve("monew.log.2026-10-31.0.log"));
        var failure = new IllegalStateException("upload failed");
        when(logStoragePort.upload(date, 1L, files)).thenThrow(failure);

        assertThatThrownBy(() -> application().backup(date, 1L, files)).isSameAs(failure);
    }

    private LogBackupApplication application() {
        return new LogBackupApplication(
                logStoragePort, logDirectory.resolve("monew.log").toString());
    }
}
