package com.sprint.monew.application.article;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.monew.global.type.ArticleSourceType;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ArticleApplicationTest {

    @Mock private ArticleService articleService;
    @Mock private ArticleViewStatusService articleViewStatusService;

    @InjectMocks private ArticleApplication articleApplication;

    @Test
    @DisplayName("출처 목록을 조회하면 등록된 출처를 전부 돌려준다")
    void whenFindSourceTypes_thenReturnAllSources() {
        List<ArticleSourceType> sources = articleApplication.findSourceTypes();

        assertThat(sources).containsExactly(ArticleSourceType.values());
    }
}
