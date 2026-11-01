package com.sprint.monew.application.article.required;

import com.sprint.monew.application.article.provided.dto.CollectedArticle;
import com.sprint.monew.global.type.ArticleSourceType;
import java.util.List;

public interface RssNewsPort {
    List<CollectedArticle> collect(ArticleSourceType source);
}
