package com.sprint.monew.global.util.backup;

import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.ArticleErrorType;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;
import org.springframework.stereotype.Component;

@Component
public class ArticleBackupGzipCompressor {

    public byte[] compress(byte[] content) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try {
            try (GZIPOutputStream gzip = new GZIPOutputStream(output)) {
                gzip.write(content);
            }
            return output.toByteArray();
        } catch (IOException e) {
            throw new MonewException(ArticleErrorType.ARTICLE_BACKUP_ZIP_FAILED, e);
        }
    }
}
