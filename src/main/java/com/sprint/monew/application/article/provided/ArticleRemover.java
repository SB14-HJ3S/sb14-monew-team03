package com.sprint.monew.application.article.provided;

import java.util.UUID;

public interface ArticleRemover {

    void softDeleteArticle(UUID articleId);

    void hardDeleteArticle(UUID articleId);
}
