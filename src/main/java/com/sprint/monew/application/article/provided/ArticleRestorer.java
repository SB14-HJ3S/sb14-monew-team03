package com.sprint.monew.application.article.provided;

import com.sprint.monew.domain.article.entity.Article;
import java.time.LocalDateTime;
import java.util.List;

public interface ArticleRestorer {

    List<Article> restoreArticles(LocalDateTime from, LocalDateTime to);
}
