package com.sprint.monew.adapter.in.controller.article;

import com.sprint.monew.adapter.in.batch.ArticleCollectionRunner;
import com.sprint.monew.global.annotation.RestWebAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@RestWebAdapter(path = "/api/dev/article-collections")
@Profile("local")
@RequiredArgsConstructor
public class ArticleCollectionControllerTest {

    private final ArticleCollectionRunner runner;

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void collect() {
        runner.runCollection();
    }
}
