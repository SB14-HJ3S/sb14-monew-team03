package com.sprint.monew.adapter.out.article.rss;

import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndEntry;
import com.sprint.monew.application.article.provided.dto.CollectedArticle;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.ArticleErrorType;
import com.sprint.monew.global.type.ArticleSourceType;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Objects;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

@Component
public class RssNewsMapper {

    public CollectedArticle toCollectedArticle(ArticleSourceType source, SyndEntry entry) {
        String sourceUrl = entry.getLink();
        if (Objects.isNull(sourceUrl) || sourceUrl.isBlank()) {
            throw new MonewException(ArticleErrorType.ARTICLE_SOURCE_URL_MISSING);
        }

        String title = entry.getTitle();
        if (Objects.isNull(title) || title.isBlank()) {
            throw new MonewException(ArticleErrorType.ARTICLE_TITLE_MISSING);
        }

        Date publishedDate = entry.getPublishedDate();
        if (Objects.isNull(publishedDate)) {
            throw new MonewException(ArticleErrorType.ARTICLE_PUBLISHED_AT_INVALID);
        }

        LocalDateTime publishedAt =
                publishedDate.toInstant().atZone(ZoneId.of("Asia/Seoul")).toLocalDateTime();

        return new CollectedArticle(
                source, sourceUrl.strip(), title.strip(), publishedAt, extractSummary(entry));
    }

    private String extractSummary(SyndEntry entry) {
        String summary = toPlainText(entry.getDescription());

        if (summary.isBlank() && Objects.nonNull(entry.getContents())) {
            for (SyndContent content : entry.getContents()) {
                summary = toPlainText(content);
                if (!summary.isBlank()) {
                    break;
                }
            }
        }
        return summary;
    }

    private String toPlainText(SyndContent content) {
        if (Objects.isNull(content) || Objects.isNull(content.getValue())) {
            return "";
        }
        Document document = Jsoup.parse(content.getValue());

        // 이미지, 캡션, 출처를 summary에서 안보이게 제거
        document.select("div.ynaimage").remove(); // 연합
        document.select("img").remove(); // 조선

        return document.text();
    }
}
