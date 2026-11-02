package com.sprint.monew.adapter.out.article.rss;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import com.sprint.monew.application.article.provided.dto.CollectedArticle;
import com.sprint.monew.application.article.required.RssNewsPort;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.CommonErrorType;
import com.sprint.monew.global.exception.errortype.ExternalErrorType;
import com.sprint.monew.global.type.ArticleSourceType;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
public class RssNewsAdapter implements RssNewsPort {

    private final RestClient restClient;
    private final RssNewsMapper mapper;
    private final Map<ArticleSourceType, String> rssFeedUrls;

    public RssNewsAdapter(
            RestClient.Builder builder,
            RssNewsMapper mapper,
            @Value("${monew.news.rss.hankyung}") String hankyungUrl,
            @Value("${monew.news.rss.chosun}") String chosunUrl,
            @Value("${monew.news.rss.yonhap}") String yonhapUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();

        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));

        this.restClient = builder.clone().requestFactory(requestFactory).build();

        this.mapper = mapper;
        this.rssFeedUrls =
                Map.of(
                        ArticleSourceType.HANKYUNG, hankyungUrl,
                        ArticleSourceType.CHOSUN, chosunUrl,
                        ArticleSourceType.YONHAP, yonhapUrl);
    }

    @Override
    public List<CollectedArticle> collect(ArticleSourceType source) {
        String rssFeedUrl = rssFeedUrls.get(source);
        if (Objects.isNull(rssFeedUrl)) {
            throw new MonewException(CommonErrorType.INVALID_REQUEST);
        }

        SyndFeed entireFeed = requestFeed(rssFeedUrl);
        List<CollectedArticle> articles = new ArrayList<>();

        for (SyndEntry articleEntry : entireFeed.getEntries()) {
            if (Objects.isNull(articleEntry)) {
                log.warn("RSS 기사 항목 없음 - 건너뜀: source={}", source);
                continue;
            }

            try {
                articles.add(mapper.toCollectedArticle(source, articleEntry));
            } catch (MonewException e) {
                log.warn("RSS 기사 변환 실패로 건너뜀: source={}, errorType={}", source, e.getErrorType(), e);
            }
        }

        return articles;
    }

    // 원본 RSS byte -> SyndFeed 객체로
    private SyndFeed requestFeed(String feedUrl) {
        byte[] body;

        try {
            body = restClient.get().uri(feedUrl).retrieve().body(byte[].class);
        } catch (RestClientException e) {
            throw new MonewException(ExternalErrorType.EXTERNAL_API_CALL_FAILED, e);
        }

        if (Objects.isNull(body) || body.length == 0) {
            throw new MonewException(ExternalErrorType.EXTERNAL_API_CALL_FAILED);
        }

        try (XmlReader reader = new XmlReader(new ByteArrayInputStream(body))) {
            SyndFeedInput input = new SyndFeedInput();
            return input.build(reader);
        } catch (IOException | FeedException e) {
            throw new MonewException(ExternalErrorType.EXTERNAL_API_CALL_FAILED, e);
        }
    }
}
