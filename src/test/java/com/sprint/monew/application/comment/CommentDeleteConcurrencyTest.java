package com.sprint.monew.application.comment;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.monew.adapter.out.article.ArticleRepositoryAdapter;
import com.sprint.monew.adapter.out.comment.CommentRepositoryAdapter;
import com.sprint.monew.adapter.out.user.UserRepositoryAdapter;
import com.sprint.monew.domain.article.entity.Article;
import com.sprint.monew.domain.comment.entity.Comment;
import com.sprint.monew.domain.user.entity.User;
import com.sprint.monew.global.type.ArticleSourceType;
import com.sprint.monew.support.IntegrationTest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

// 여러 스레드가 각자 트랜잭션을 커밋해야 해서 @Transactional을 안 붙이고, 끝나면 만든 데이터를 직접 지운다
@IntegrationTest
class CommentDeleteConcurrencyTest {

    private static final int REQUEST_COUNT = 10;
    // 0 아래로 안 내려가는 조건에 가려지지 않게 넉넉히 둔다
    private static final int INITIAL_COMMENT_COUNT = 5;

    @Autowired private CommentApplication commentApplication;
    @Autowired private CommentRepositoryAdapter commentRepository;
    @Autowired private UserRepositoryAdapter userRepository;
    @Autowired private ArticleRepositoryAdapter articleRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private UUID userId;
    private UUID articleId;
    private UUID commentId;

    @BeforeEach
    void setUp() {
        User author =
                userRepository.save(
                        User.create(
                                "author-" + UUID.randomUUID() + "@monew.com",
                                "author",
                                "password1!"));
        userId = author.getId();
        Article article =
                articleRepository.save(
                        Article.create(
                                ArticleSourceType.NAVER,
                                "https://news.example.com/" + UUID.randomUUID(),
                                "기사 제목",
                                LocalDateTime.now(),
                                "기사 요약"));
        articleId = article.getId();
        commentId = commentRepository.save(Comment.create(author, article, "댓글 내용")).getId();
        jdbcTemplate.update(
                "UPDATE articles SET comment_count = ? WHERE id = ?",
                INITIAL_COMMENT_COUNT,
                articleId);
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM comment_likes WHERE comment_id = ?", commentId);
        jdbcTemplate.update("DELETE FROM comments WHERE id = ?", commentId);
        jdbcTemplate.update("DELETE FROM articles WHERE id = ?", articleId);
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", userId);
    }

    @Test
    @DisplayName("같은 댓글에 논리 삭제 요청이 동시에 여러 번 와도 기사의 댓글 수는 1만 줄어든다")
    void concurrentSoftDeletes() throws Exception {
        runConcurrently(i -> commentApplication.softDeleteComment(commentId));

        assertThat(commentCount()).isEqualTo(INITIAL_COMMENT_COUNT - 1);
    }

    @Test
    @DisplayName("같은 댓글에 논리 삭제와 물리 삭제가 동시에 와도 기사의 댓글 수는 1만 줄어들고 댓글은 지워진다")
    void concurrentSoftAndHardDeletes() throws Exception {
        runConcurrently(
                i -> {
                    if (i % 2 == 0) {
                        commentApplication.softDeleteComment(commentId);
                    } else {
                        commentApplication.hardDeleteComment(commentId);
                    }
                });

        assertThat(commentCount()).isEqualTo(INITIAL_COMMENT_COUNT - 1);
        assertThat(commentRepository.findById(commentId)).isEmpty();
    }

    // 모든 스레드가 준비되면 한꺼번에 출발시킨다
    // 늦게 온 요청은 404 등으로 실패하는 게 정상이라 예외는 무시하고, 결과는 DB 값으로만 본다
    private void runConcurrently(DeleteRequest request) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(REQUEST_COUNT);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> results = new ArrayList<>();
        for (int i = 0; i < REQUEST_COUNT; i++) {
            int index = i;
            results.add(
                    executor.submit(
                            () -> {
                                start.await();
                                request.run(index);
                                return null;
                            }));
        }
        start.countDown();
        for (Future<?> result : results) {
            try {
                result.get(30, TimeUnit.SECONDS);
            } catch (ExecutionException ignored) {
                // 먼저 지운 요청에 밀린 요청
            }
        }
        executor.shutdown();
    }

    private Integer commentCount() {
        return jdbcTemplate.queryForObject(
                "SELECT comment_count FROM articles WHERE id = ?", Integer.class, articleId);
    }

    @FunctionalInterface
    private interface DeleteRequest {
        void run(int index);
    }
}
