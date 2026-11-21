package com.sprint.monew.application.article.required;

import com.sprint.monew.application.article.provided.usecase.dto.CollectedArticle;
import java.util.List;

public interface NaverNewsPort {
    List<CollectedArticle> search(String keyword);
}
