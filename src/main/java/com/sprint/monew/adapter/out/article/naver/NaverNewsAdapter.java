package com.sprint.monew.adapter.out.article.naver;

import com.sprint.monew.application.article.provided.dto.CollectedArticle;
import com.sprint.monew.application.article.required.NaverNewsPort;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.ExternalErrorType;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class NaverNewsAdapter implements NaverNewsPort {

    // NAVER 제공
    private static final int PAGE_SIZE = 100; // api 1번 호출할 때 가져오는 기사 수
    private static final int MAX_RESULTS = 1000; // 하나의 키워드에 대해 최대 몇번째 결과까지 조회할지
    // 즉, 키워드당 GET 한 번만 하는 것이 아니라, 100개씩 최대 10번 요청

    private final RestClient restClient;
    private final NaverNewsMapper mapper;
    private final String newsUrl;

    public NaverNewsAdapter(
            RestClient.Builder builder,
            NaverNewsMapper mapper,
            @Value("${monew.naver.news-url}") String newsUrl,
            @Value("${monew.naver.client-id}") String clientId,
            @Value("${monew.naver.client-secret}") String clientSecret) {

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();

        requestFactory.setConnectTimeout(Duration.ofSeconds(3)); // 네이버 서버 연결 대기
        requestFactory.setReadTimeout(Duration.ofSeconds(10)); // 네이버 서버 응답 대기

        this.restClient =
                builder.clone()
                        .requestFactory(requestFactory)
                        .defaultHeader("X-NCP-APIGW-API-KEY-ID", clientId)
                        .defaultHeader("X-NCP-APIGW-API-KEY", clientSecret)
                        .build();

        this.mapper = mapper;
        this.newsUrl = newsUrl;
    }

    // 반복 NAVER API GET 호출 - 키워드에 대한 기사 다 받을 때까지
    @Override
    public List<CollectedArticle> search(String keyword) {
        List<CollectedArticle> articles = new ArrayList<>();

        for (int start = 1; start <= MAX_RESULTS; start += PAGE_SIZE) {
            NaverNewsResponse response = requestPage(keyword, start);

            if (Objects.isNull(response)
                    || Objects.isNull(response.items())
                    || response.items().isEmpty()) {
                break;
            }

            for (NaverNewsResponse.ArticleItem item : response.items()) {
                articles.add(mapper.toCollectedArticle(item));
            }

            if (response.items().size() < PAGE_SIZE) {
                break;
            }

            if (start + response.items().size() > response.total()) {
                break;
            }
        }

        return articles;
    }

    private NaverNewsResponse requestPage(String keyword, int start) {
        URI uri =
                UriComponentsBuilder.fromUriString(newsUrl)
                        .queryParam("query", "{keyword}")
                        .queryParam("display", PAGE_SIZE)
                        .queryParam("start", start)
                        .queryParam("sort", "date")
                        .queryParam("format", "json")
                        .encode()
                        .build()
                        .expand(keyword.trim())
                        .toUri();

        try {
            return restClient
                    .get()
                    .uri(uri)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(NaverNewsResponse.class);
        } catch (RestClientException e) {
            throw new MonewException(ExternalErrorType.EXTERNAL_API_CALL_FAILED, e);
        }
    }
}
