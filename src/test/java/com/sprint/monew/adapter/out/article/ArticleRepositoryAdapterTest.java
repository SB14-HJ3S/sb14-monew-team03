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
    void givenExistingArticle_whenIncreaseCommentCount_thenIncrementAndReturnOne() {
        // given
        UUID articleId = savedArticleId();

        // when
        int updated = articleRepository.increaseCommentCount(articleId);

        // then
        assertThat(updated).isEqualTo(1);
        assertThat(commentCountOf(articleId)).isEqualTo(1);
    }

    @Test
    @DisplayName("없는 기사면 바뀐 줄 수 0을 돌려준다")
    void givenMissingArticle_whenIncreaseCommentCount_thenReturnZero() {
        // given
        UUID articleId = UUID.randomUUID();

        // when
        int updated = articleRepository.increaseCommentCount(articleId);

        // then
        assertThat(updated).isZero();
    }

    @Test
    @DisplayName("댓글 수가 1 이상이면 DB의 댓글 수가 1 줄고, 바뀐 줄 수 1을 돌려준다")
    void givenPositiveCommentCount_whenDecreaseCommentCount_thenDecrementAndReturnOne() {
        // given
        UUID articleId = savedArticleId();
        articleRepository.increaseCommentCount(articleId);

        // when
        int updated = articleRepository.decreaseCommentCount(articleId);

        // then
        assertThat(updated).isEqualTo(1);
        assertThat(commentCountOf(articleId)).isZero();
    }

    @Test
    @DisplayName("댓글 수가 0이면 줄여도 0 그대로고, 바뀐 줄 수 0을 돌려준다")
    void givenZeroCommentCount_whenDecreaseCommentCount_thenKeepZeroAndReturnZero() {
        // given
        UUID articleId = savedArticleId();

        // when
        int updated = articleRepository.decreaseCommentCount(articleId);

        // then
        assertThat(updated).isZero();
        assertThat(commentCountOf(articleId)).isZero();
    }
}
