package com.sprint.monew.application.article;

import com.sprint.monew.application.article.required.ArticleRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ArticleService implements IArticleService {

    private final ArticleRepositoryPort repository;
}
