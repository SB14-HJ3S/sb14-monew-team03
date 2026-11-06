package com.sprint.monew.adapter.in.controller.article;

import com.sprint.monew.application.article.provided.ArticleBackupUseCase;
import com.sprint.monew.application.article.provided.dto.ArticleBackupResult;
import com.sprint.monew.application.article.provided.dto.ArticleResult;
import com.sprint.monew.global.annotation.RestWebAdapter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@Profile("local")
@RestWebAdapter(path = "/api/dev/article-backups")
@RequiredArgsConstructor
public class ArticleBackupManualController {

    private final ArticleBackupUseCase articleBackupUseCase;

    @PostMapping
    public List<ArticleBackupResult> backup(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "limit", defaultValue = "1000") int limit) {
        if (limit < 1 || limit > 1000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "조회 개수는 1~1000이어야 합니다.");
        }

        List<ArticleBackupResult> results = new ArrayList<>();

        UUID afterId = null;
        long partNumber = 1;

        while (true) {
            // 1. 이전에 읽은 기사 다음부터 조회
            List<ArticleResult> articles = articleBackupUseCase.readArticles(date, afterId, limit);

            // 2. 더 이상 없으면 종료
            if (articles.isEmpty()) {
                break;
            }

            // 3. JSON 변환 → GZIP 압축 → S3 저장
            List<ArticleBackupResult> files =
                    articleBackupUseCase.backupChunk(date, partNumber, articles);

            results.addAll(files);

            // 4. 저장 성공 후 책갈피와 파일 번호 갱신
            afterId = articles.get(articles.size() - 1).id();
            partNumber++;
        }

        return results;
    }
}
