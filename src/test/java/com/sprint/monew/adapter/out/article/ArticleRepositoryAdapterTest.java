package com.sprint.monew.adapter.out.article;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.monew.domain.article.entity.Article;
import com.sprint.monew.global.type.ArticleSourceType;
import com.sprint.monew.support.IntegrationTest;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class ArticleRepositoryAdapterTest {

    @Autowired private ArticleRepositoryAdapter articleRepository;

    private UUID savedArticleId() {
        return articleRepository
                .save(
                        Article.create(
                                ArticleSourceType.NAVER,
                                "https://news.example.com/comment-count",
                                "기사 제목",
                                LocalDateTime.now(),
                                "기사 요약"))
                .getId();
    }

    private int commentCountOf(UUID articleId) {
        return articleRepository.findById(articleId).orElseThrow().getCommentCount();
    }

    @Test
    @DisplayName("댓글 수를 늘리면 DB의 댓글 수가 1 늘고, 바뀐 줄 수 1을 돌려준다")
    void increaseCommentCount() {
        UUID articleId = savedArticleId();

        int updated = articleRepository.increaseCommentCount(articleId);

        assertThat(updated).isEqualTo(1);
        assertThat(commentCountOf(articleId)).isEqualTo(1);
    }

    @Test
    @DisplayName("없는 기사면 바뀐 줄 수 0을 돌려준다")
    void increaseCommentCountNotExist() {
        int updated = articleRepository.increaseCommentCount(UUID.randomUUID());

        assertThat(updated).isZero();
    }
}
