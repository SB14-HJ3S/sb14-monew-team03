package com.sprint.monew.application.article;

import com.sprint.monew.application.article.provided.dto.ArticleBackupFile;
import com.sprint.monew.application.article.provided.dto.ArticleBackupItem;
import com.sprint.monew.application.article.provided.dto.ArticleBackupResult;
import com.sprint.monew.application.article.required.ArticleBackupStoragePort;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ArticleBackupStorageServiceImpl implements ArticleBackupStorageService {

    private final ArticleBackupStoragePort articleBackupStoragePort;

    @Override
    public ArticleBackupResult save(long partNumber, ArticleBackupFile backupFile) {
        return articleBackupStoragePort.save(partNumber, backupFile);
    }

    @Override
    public List<String> findKeys(LocalDate date) {
        return articleBackupStoragePort.findKeys(date);
    }

    @Override
    public List<ArticleBackupItem> readFile(String key) {
        return articleBackupStoragePort.readFile(key);
    }

    @Override
    public List<ArticleBackupItem> readAll(LocalDate date) {
        return articleBackupStoragePort.readAll(date);
    }
}
