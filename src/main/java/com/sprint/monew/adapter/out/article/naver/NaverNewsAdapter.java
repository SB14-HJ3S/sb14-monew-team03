package com.sprint.monew.adapter.out.article.naver;

import com.sprint.monew.application.article.provided.dto.CollectedArticle;
import com.sprint.monew.application.article.required.NaverNewsPort;
import java.net.URI;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class NaverNewsAdapter implements NaverNewsPort {

    private final RestClient restClient;
    private final NaverNewsMapper mapper;
    private final String newsUrl;

    public NaverNewsAdapter(
            RestClient.Builder builder,
            NaverNewsMapper mapper,
            @Value("${monew.naver.news-url}") String newsUrl,
            @Value("${monew.naver.client-id}") String clientId,
            @Value("${monew.naver.client-secret}") String clientSecret) {
        this.restClient =
                builder.defaultHeader("X-NCP-APIGW-API-KEY-ID", clientId)
                        .defaultHeader("X-NCP-APIGW-API-KEY", clientSecret)
                        .build();
        this.mapper = mapper;
        this.newsUrl = newsUrl;
    }

    @Override
    public List<CollectedArticle> search(String keyword) {
        URI uri =
                UriComponentsBuilder.fromUriString(newsUrl)
                        .queryParam("query", "{keyword}")
                        .queryParam("display", 10)
                        .queryParam("start", 1)
                        .queryParam("sort", "date")
                        .queryParam("format", "json")
                        .encode()
                        .build()
                        .expand(keyword.trim())
                        .toUri();
        NaverNewsResponse response =
                restClient
                        .get()
                        .uri(uri)
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .body(NaverNewsResponse.class);

        return response.items().stream().map(mapper::toCollectedArticle).toList();
    }
}
