package com.sprint.monew.adapter.out.article.naver;

import com.sprint.monew.application.article.provided.dto.CollectedArticle;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.ArticleErrorType;
import com.sprint.monew.global.type.ArticleSourceType;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.stereotype.Component;

@Component
public class NaverNewsMapper {

    public CollectedArticle toCollectedArticle(NaverNewsResponse.ArticleItem articleItem) {
        String sourceUrl = articleItem.originallink();

        if (Objects.isNull(sourceUrl) || sourceUrl.isBlank()) {
            sourceUrl = articleItem.link();
            if (Objects.isNull(sourceUrl) || sourceUrl.isBlank()) {
                throw new MonewException(ArticleErrorType.SOURCE_URL_IS_EMPTY);
            }
        }

        String title = sanitize(articleItem.title());
        if (title.isBlank()) {
            throw new MonewException(ArticleErrorType.TITLE_IS_EMPTY);
        }

        LocalDateTime publishedAt =
                ZonedDateTime.parse(articleItem.pubDate(), DateTimeFormatter.RFC_1123_DATE_TIME)
                        .toLocalDateTime();

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
        String highlightRemoved = value.replace("<b>", "").replace("</b>", "");

        return StringEscapeUtils.unescapeHtml4(highlightRemoved).strip();
    }
}
