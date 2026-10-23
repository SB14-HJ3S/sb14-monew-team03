package com.sprint.monew.application.comment;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.monew.adapter.out.article.ArticleRepositoryAdapter;
import com.sprint.monew.adapter.out.comment.CommentRepositoryAdapter;
import com.sprint.monew.adapter.out.user.UserRepositoryAdapter;
import com.sprint.monew.domain.article.entity.Article;
import com.sprint.monew.domain.comment.entity.Comment;
import com.sprint.monew.domain.user.entity.User;
import com.sprint.monew.global.context.RequestIdContext;
import com.sprint.monew.global.type.ArticleSourceType;
import com.sprint.monew.support.IntegrationTest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

// 여러 스레드가 각자 트랜잭션을 커밋해야 해서 @Transactional을 안 붙이고, 끝나면 만든 데이터를 직접 지운다
@IntegrationTest
class CommentLikeConcurrencyTest {

    private static final int USER_COUNT = 20;

    @Autowired private CommentApplication commentApplication;
    @Autowired private CommentRepositoryAdapter commentRepository;
    @Autowired private UserRepositoryAdapter userRepository;
    @Autowired private ArticleRepositoryAdapter articleRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private final List<UUID> userIds = new ArrayList<>();
    private UUID articleId;
    private UUID commentId;

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM comment_likes WHERE comment_id = ?", commentId);
        jdbcTemplate.update("DELETE FROM comments WHERE id = ?", commentId);
        jdbcTemplate.update("DELETE FROM articles WHERE id = ?", articleId);
        userIds.forEach(id -> jdbcTemplate.update("DELETE FROM users WHERE id = ?", id));
    }

    @Test
    @DisplayName("사용자 여러 명이 같은 댓글에 동시에 좋아요를 눌러도 좋아요 수가 사람 수만큼 늘어난다")
    void concurrentLikes() throws Exception {
        User author = saveUser("author");
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
        List<UUID> likerIds = new ArrayList<>();
        for (int i = 0; i < USER_COUNT; i++) {
            likerIds.add(saveUser("liker" + i).getId());
        }

        // 모든 스레드가 준비되면 한꺼번에 출발시켜 동시에 누르게 한다
        ExecutorService executor = Executors.newFixedThreadPool(USER_COUNT);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> results = new ArrayList<>();
        for (UUID likerId : likerIds) {
            results.add(
                    executor.submit(
                            () -> {
                                start.await();
                                RequestIdContext.set(likerId);
                                try {
                                    commentApplication.like(commentId);
                                } finally {
                                    RequestIdContext.clear();
                                }
                                return null;
                            }));
        }
        start.countDown();
        for (Future<?> result : results) {
            result.get(30, TimeUnit.SECONDS);
        }
        executor.shutdown();

        Long likeRows =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM comment_likes WHERE comment_id = ?",
                        Long.class,
                        commentId);
        assertThat(likeRows).isEqualTo(USER_COUNT);
        assertThat(commentRepository.findById(commentId).orElseThrow().getLikeCount())
                .isEqualTo(USER_COUNT);
    }

    private User saveUser(String name) {
        User user =
                userRepository.save(
                        User.create(
                                name + "-" + UUID.randomUUID() + "@monew.com", name, "password1!"));
        userIds.add(user.getId());
        return user;
    }
}
