package com.sprint.monew.application.log;

import com.sprint.monew.application.log.provided.LogBackupUseCase;
import com.sprint.monew.application.log.provided.dto.LogBackupResult;
import com.sprint.monew.application.log.required.LogStoragePort;
import com.sprint.monew.global.annotation.ApplicationService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Value;

@ApplicationService
public class LogBackupApplication implements LogBackupUseCase {

    private final LogStoragePort logStoragePort;
    private final Path logFile;

    public LogBackupApplication(
            LogStoragePort logStoragePort, @Value("${logging.file.name}") String logFileName) {
        this.logStoragePort = logStoragePort;
        this.logFile = Path.of(logFileName).normalize();
    }

    @Override
    public LogBackupResult backup(LocalDate targetDate) throws IOException {
        // 날짜가 바뀐 뒤 첫 로그 기록 시 Logback이 전날 파일을 닫고 롤오버한다.
        List<Path> files = findLogFiles(targetDate);

        int uploadedFileCount = 0;
        for (Path file : files) {
            if (logStoragePort.upload(file)) {
                uploadedFileCount++;
            }
        }

        return new LogBackupResult(files.size(), uploadedFileCount);
    }

    private List<Path> findLogFiles(LocalDate targetDate) throws IOException {
        Pattern fileNamePattern =
                Pattern.compile(
                        Pattern.quote(logFile.getFileName() + "." + targetDate) + "\\.\\d+\\.log");

        try (Stream<Path> paths = Files.list(logFile.getParent())) {
            return paths.filter(Files::isRegularFile)
                    .filter(
                            path ->
                                    fileNamePattern
                                            .matcher(path.getFileName().toString())
                                            .matches())
                    .sorted()
                    .toList();
        }
    }
}
