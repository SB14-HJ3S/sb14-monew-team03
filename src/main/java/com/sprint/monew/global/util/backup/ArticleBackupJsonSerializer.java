package com.sprint.monew.global.util.backup;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.monew.application.article.provided.dto.ArticleBackupFile;
import com.sprint.monew.application.article.provided.dto.ArticleBackupItem;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.ArticleErrorType;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ArticleBackupJsonSerializer {

    private final ObjectMapper objectMapper;

    public byte[] serialize(ArticleBackupFile backupFile) {
        try {
            return objectMapper.writeValueAsBytes(backupFile.items()); // 기사만
        } catch (JsonProcessingException e) {
            throw new MonewException(ArticleErrorType.JSON_CONVERSION_FAILED, e);
        }
    }

    public List<ArticleBackupItem> deserialize(byte[] content) {
        try {
            return objectMapper.readValue(content, new TypeReference<List<ArticleBackupItem>>() {});
        } catch (IOException e) {
            throw new MonewException(ArticleErrorType.JSON_CONVERSION_FAILED, e);
        }
    }
}
