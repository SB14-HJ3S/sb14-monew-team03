package com.sprint.monew.adapter.out.article.naver;

import com.sprint.monew.application.article.provided.dto.CollectedArticle;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.ArticleErrorType;
import com.sprint.monew.global.type.ArticleSourceType;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

@Component
public class NaverNewsMapper {

    public CollectedArticle toCollectedArticle(NaverNewsResponse.ArticleItem articleItem) {
        String sourceUrl = articleItem.originallink();

        if (Objects.isNull(sourceUrl) || sourceUrl.isBlank()) {
            sourceUrl = articleItem.link();
            if (Objects.isNull(sourceUrl) || sourceUrl.isBlank()) {
                throw new MonewException(ArticleErrorType.ARTICLE_SOURCE_URL_MISSING);
            }
        }

        String title = sanitize(articleItem.title());
        if (title.isBlank()) {
            throw new MonewException(ArticleErrorType.ARTICLE_TITLE_MISSING);
        }

        String date = articleItem.pubDate();
        if (date == null) {
            throw new MonewException(ArticleErrorType.ARTICLE_PUBLISHED_AT_INVALID);
        }
        if (date.contains(",")) {
            date = date.substring(date.indexOf(',') + 1).strip();
        }
        LocalDateTime publishedAt;
        try {
            publishedAt =
                    ZonedDateTime.parse(date, DateTimeFormatter.RFC_1123_DATE_TIME)
                            .withZoneSameInstant(ZoneId.of("Asia/Seoul"))
                            .toLocalDateTime();
        } catch (DateTimeParseException e) {
            throw new MonewException(ArticleErrorType.ARTICLE_PUBLISHED_AT_INVALID, e);
        }

        return new CollectedArticle(
                ArticleSourceType.NAVER,
                sourceUrl.trim(),
                title,
                publishedAt,
                sanitize(articleItem.description()));
    }

    private String sanitize(String value) {
        if (Objects.isNull(value)) {
            return "";
        }
        return Jsoup.parse(value).text().strip();
    }
}
