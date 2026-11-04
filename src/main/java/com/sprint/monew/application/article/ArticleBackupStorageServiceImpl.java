package com.sprint.monew.application.article;

import com.sprint.monew.application.article.provided.dto.ArticleBackupFile;
import com.sprint.monew.application.article.provided.dto.ArticleBackupResult;
import com.sprint.monew.application.article.required.ArticleBackupStoragePort;
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
}
