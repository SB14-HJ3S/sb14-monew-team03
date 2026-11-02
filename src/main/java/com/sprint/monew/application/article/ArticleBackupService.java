package com.sprint.monew.application.article;

import com.sprint.monew.domain.article.entity.Article;
import java.time.LocalDate;
import java.util.List;

public interface ArticleBackupService {

    List<Article> getArticlesToBackup(LocalDate date);
}
