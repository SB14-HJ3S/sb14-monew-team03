package com.sprint.monew.application.article.provided;

import com.sprint.monew.domain.article.entity.ArticleViewStatus;
import java.util.UUID;

public interface ArticleViewStatusRegister {

    ArticleViewStatus register(UUID requestId, UUID articleId);
}
