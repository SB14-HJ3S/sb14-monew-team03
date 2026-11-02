package com.sprint.monew.adapter.out.article;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.monew.application.article.provided.dto.ArticleBackupFile;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.ArticleErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ArticleBackupJsonSerializer {

    private final ObjectMapper objectMapper;

    public byte[] serialize(ArticleBackupFile backupFile) {
        try {
            return objectMapper.writeValueAsBytes(backupFile);
        } catch (JsonProcessingException e) {
            throw new MonewException(ArticleErrorType.JSON_CONVERSION_FAILED, e);
        }
    }
}
