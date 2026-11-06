package com.sprint.monew.application.log;

import com.sprint.monew.application.log.provided.LogBackupUseCase;
import com.sprint.monew.application.log.provided.dto.LogBackupResult;
import com.sprint.monew.application.log.provided.dto.LogResult;
import com.sprint.monew.application.log.required.LogStoragePort;
import com.sprint.monew.global.annotation.ApplicationService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Comparator;
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
    public LogResult readLogs(LocalDate targetDate, int afterIndex, int limit) throws IOException {
        return LogResult.from(findLogFiles(targetDate, afterIndex, limit));
    }

    @Override
    public LogBackupResult backup(LocalDate targetDate, long partNumber, List<Path> logPath) {
        int uploadedFileCount = 0;
        boolean uploaded = logStoragePort.upload(targetDate, partNumber, logPath);
        if (uploaded) {
            uploadedFileCount += logPath.size();
        }

        return new LogBackupResult(logPath.size(), uploadedFileCount);
    }

    private List<Path> findLogFiles(LocalDate targetDate, int afterIndex, int limit)
            throws IOException {
        Pattern fileNamePattern =
                Pattern.compile(
                        Pattern.quote(logFile.getFileName() + "." + targetDate)
                                + "\\.(\\d+)\\.log");

        Path logDirectory = logFile.getParent();

        try (Stream<Path> paths = Files.list(logDirectory)) {
            return paths.filter(Files::isRegularFile)
                    .filter(
                            path ->
                                    fileNamePattern
                                            .matcher(path.getFileName().toString())
                                            .matches())
                    .filter(path -> getIndex(path) > afterIndex)
                    .sorted(Comparator.comparingInt(this::getIndex))
                    .limit(limit)
                    .toList();
        }
    }

    private int getIndex(Path path) {
        String[] parts = path.getFileName().toString().split("\\.");
        return Integer.parseInt(parts[parts.length - 2]);
    }
}
