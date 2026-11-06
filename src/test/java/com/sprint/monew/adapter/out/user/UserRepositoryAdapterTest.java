package com.sprint.monew.adapter.out.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.monew.domain.article.entity.Article;
import com.sprint.monew.domain.article.entity.ArticleViewStatus;
import com.sprint.monew.domain.comment.entity.Comment;
import com.sprint.monew.domain.comment.entity.CommentLike;
import com.sprint.monew.domain.notification.entity.Notification;
import com.sprint.monew.domain.user.entity.User;
import com.sprint.monew.global.type.ArticleSourceType;
import com.sprint.monew.global.type.ResourceType;
import com.sprint.monew.support.IntegrationTest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class UserRepositoryAdapterTest {

    @Autowired private UserRepositoryAdapter userRepository;

    @Autowired private EntityManager entityManager;

    @Test
    @DisplayName("탈퇴하지 않은 사용자는 조회된다")
    void givenActiveUser_whenFindUser_thenFound() {
        User saved = userRepository.save(User.create("active@monew.com", "활성", "password1!"));

        assertThat(userRepository.findUser(saved.getId())).isPresent();
    }

    @Test
    @DisplayName("탈퇴(논리 삭제)한 사용자는 조회되지 않는다")
    void givenDeletedUser_whenFindUser_thenEmpty() {
        User saved = userRepository.save(User.create("deleted@monew.com", "탈퇴", "password1!"));
        saved.softDelete();

        assertThat(userRepository.findUser(saved.getId())).isEmpty();
    }

    @Test
    @DisplayName("탈퇴하지 않은 사용자는 이메일로 조회된다")
    void givenActiveUser_whenFindUserByEmail_thenFound() {
        userRepository.save(User.create("hello@monew.com", "활성", "password1!"));

        assertThat(userRepository.findUserByEmail("hello@monew.com")).isPresent();
    }

    @Test
    @DisplayName("탈퇴(논리 삭제)한 사용자는 이메일로도 조회되지 않는다 — 로그인 차단")
    void givenDeletedUser_whenFindUserByEmail_thenEmpty() {
        User saved = userRepository.save(User.create("bye@monew.com", "탈퇴", "password1!"));
        saved.softDelete();

        assertThat(userRepository.findUserByEmail("bye@monew.com")).isEmpty();
    }

    @Test
    @DisplayName("탈퇴한 사용자의 이메일도 이미 있는 이메일로 본다 — 재가입 중복 확인")
    void givenDeletedUser_whenExistsByEmail_thenTrue() {
        User saved = userRepository.save(User.create("left@monew.com", "탈퇴", "password1!"));
        saved.softDelete();

        assertThat(userRepository.existsByEmail("left@monew.com")).isTrue();
    }

    @Test
    @DisplayName("여러 id로 조회하면 탈퇴했거나 없는 사용자는 빼고 활성 사용자만 찾는다")
    void givenActiveDeletedAndMissingIds_whenFindUsers_thenOnlyActive() {
        User first = userRepository.save(User.create("first@monew.com", "첫째", "password1!"));
        User second = userRepository.save(User.create("second@monew.com", "둘째", "password1!"));
        User deleted = userRepository.save(User.create("gone@monew.com", "탈퇴", "password1!"));
        deleted.softDelete();

        List<User> result =
                userRepository.findUsers(
                        List.of(first.getId(), second.getId(), deleted.getId(), UUID.randomUUID()));

        assertThat(result).containsExactlyInAnyOrder(first, second);
    }

    @Test
    @DisplayName("탈퇴한 사용자도 findUserIncludingDeleted로 찾는다")
    void givenDeletedUser_whenFindUserIncludingDeleted_thenFound() {
        User saved = userRepository.save(User.create("withdrawn@monew.com", "탈퇴자", "password1!"));
        saved.softDelete();
        entityManager.flush();
        entityManager.clear();

        assertThat(userRepository.findUserIncludingDeleted(saved.getId())).isPresent();
        assertThat(userRepository.findUser(saved.getId())).isEmpty();
    }

    @Test
    @DisplayName("탈퇴한 사용자도 findUserIncludingDeletedForUpdate로 잠그고 찾는다")
    void givenDeletedUser_whenFindUserIncludingDeletedForUpdate_thenFound() {
        User saved = userRepository.save(User.create("locked@monew.com", "잠금", "password1!"));
        saved.softDelete();
        entityManager.flush();
        entityManager.clear();

        User locked = userRepository.findUserIncludingDeletedForUpdate(saved.getId()).orElseThrow();

        assertThat(entityManager.getLockMode(locked)).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
    }

    @Test
    @DisplayName("사용자를 물리 삭제하면 그 사용자의 알림도 DB에서 같이 지워진다(ON DELETE CASCADE)")
    void givenUserWithNotification_whenDeleteById_thenNotificationDeleted() {
        User saved = userRepository.save(User.create("hard@monew.com", "완전삭제", "password1!"));
        Notification notification =
                Notification.create(saved, ResourceType.COMMENT, UUID.randomUUID(), "알림");
        entityManager.persist(notification);
        entityManager.flush();
        entityManager.clear();

        userRepository.deleteById(saved.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(userRepository.findById(saved.getId())).isEmpty();
        assertThat(entityManager.find(Notification.class, notification.getId())).isNull();
    }

    @Test
    @DisplayName("다른 사람 댓글에 좋아요를 누른 사용자도 물리 삭제할 수 있다 — 좋아요 때문에 삭제가 막히지 않는다")
    void givenUserWhoLikedComment_whenDeleteById_thenDeleted() {
        User writer = userRepository.save(User.create("writer@monew.com", "작성자", "password1!"));
        User liker = userRepository.save(User.create("liker@monew.com", "좋아요", "password1!"));
        Article article =
                Article.create(
                        ArticleSourceType.NAVER,
                        "https://news.example.com/1",
                        "기사 제목",
                        LocalDateTime.of(2026, 10, 22, 9, 0),
                        "기사 요약");
        entityManager.persist(article);
        Comment comment = Comment.create(writer, article, "댓글");
        entityManager.persist(comment);
        entityManager.persist(CommentLike.create(liker, comment));
        entityManager.flush();
        entityManager.clear();

        userRepository.deleteById(liker.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(userRepository.findById(liker.getId())).isEmpty();
        assertThat(entityManager.find(Comment.class, comment.getId())).isNotNull();
    }

    @Test
    @DisplayName("사용자를 물리 삭제하면 그 사용자가 쓴 댓글과 그 댓글에 달린 좋아요도 DB에서 같이 지워진다")
    void givenUserWithWrittenComment_whenDeleteById_thenCommentAndLikesDeleted() {
        User writer = userRepository.save(User.create("author@monew.com", "작성자", "password1!"));
        User liker = userRepository.save(User.create("fan@monew.com", "좋아요", "password1!"));
        Article article = saveArticle("https://news.example.com/2");
        Comment comment = Comment.create(writer, article, "댓글");
        entityManager.persist(comment);
        CommentLike like = CommentLike.create(liker, comment);
        entityManager.persist(like);
        entityManager.flush();
        entityManager.clear();

        userRepository.deleteById(writer.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(entityManager.find(Comment.class, comment.getId())).isNull();
        assertThat(entityManager.find(CommentLike.class, like.getId())).isNull();
        assertThat(entityManager.find(Article.class, article.getId())).isNotNull();
        assertThat(userRepository.findById(liker.getId())).isPresent();
    }

    @Test
    @DisplayName("사용자를 물리 삭제하면 그 사용자의 기사 조회 기록도 DB에서 같이 지워진다")
    void givenUserWithArticleView_whenDeleteById_thenArticleViewStatusDeleted() {
        User viewer = userRepository.save(User.create("viewer@monew.com", "조회자", "password1!"));
        Article article = saveArticle("https://news.example.com/3");
        ArticleViewStatus viewStatus =
                ArticleViewStatus.create(viewer, article, LocalDateTime.of(2026, 10, 22, 10, 0));
        entityManager.persist(viewStatus);
        entityManager.flush();
        entityManager.clear();

        userRepository.deleteById(viewer.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(entityManager.find(ArticleViewStatus.class, viewStatus.getId())).isNull();
        assertThat(entityManager.find(Article.class, article.getId())).isNotNull();
    }

    private Article saveArticle(String sourceUrl) {
        Article article =
                Article.create(
                        ArticleSourceType.NAVER,
                        sourceUrl,
                        "기사 제목",
                        LocalDateTime.of(2026, 10, 22, 9, 0),
                        "기사 요약");
        entityManager.persist(article);
        return article;
    }

    @Test
    @DisplayName("기준 시각 이전에 탈퇴한 사용자 id만 찾는다 — 최근 탈퇴자·정상 사용자는 빠진다")
    void givenMixedUsers_whenFindDeletedUserIdsBefore_thenOnlyOldDeletedUserIds() {
        Instant now = Instant.now();
        User oldDeleted = userRepository.save(User.create("old@monew.com", "오래전탈퇴", "password1!"));
        ReflectionTestUtils.setField(oldDeleted, "deletedAt", now.minus(Duration.ofDays(2)));
        User recentDeleted =
                userRepository.save(User.create("recent@monew.com", "최근탈퇴", "password1!"));
        ReflectionTestUtils.setField(recentDeleted, "deletedAt", now.minus(Duration.ofHours(1)));
        userRepository.save(User.create("stay@monew.com", "정상", "password1!"));
        entityManager.flush();
        entityManager.clear();

        List<UUID> result = userRepository.findDeletedUserIdsBefore(now.minus(Duration.ofDays(1)));

        assertThat(result).containsExactly(oldDeleted.getId());
    }

    @Test
    @DisplayName("ID 목록 중 탈퇴한 사용자만 잠그고 찾는다 — 정상 사용자와 없는 ID는 빠진다")
    void givenMixedIds_whenFindDeletedUsersForUpdate_thenLockOnlyDeletedUsers() {
        User deleted =
                userRepository.save(User.create("lock-gone@monew.com", "잠금탈퇴", "password1!"));
        deleted.softDelete();
        User active = userRepository.save(User.create("lock-stay@monew.com", "잠금정상", "password1!"));
        entityManager.flush();
        entityManager.clear();

        List<User> result =
                userRepository.findDeletedUsersForUpdate(
                        List.of(deleted.getId(), active.getId(), UUID.randomUUID()));

        assertThat(result).extracting(User::getId).containsExactly(deleted.getId());
        assertThat(entityManager.getLockMode(result.get(0)))
                .isEqualTo(LockModeType.PESSIMISTIC_WRITE);
    }

    @Test
    @DisplayName("ID 목록의 사용자를 한 번에 지우고 지운 수를 돌려주며, 그 사용자의 알림도 DB에서 같이 지워진다")
    void givenUsers_whenDeleteUsers_thenDeleteAllAndCascadeNotifications() {
        User first = userRepository.save(User.create("bulk1@monew.com", "일괄삭제1", "password1!"));
        User second = userRepository.save(User.create("bulk2@monew.com", "일괄삭제2", "password1!"));
        User stay = userRepository.save(User.create("bulk-stay@monew.com", "남는사람", "password1!"));
        Notification notification =
                Notification.create(first, ResourceType.INTEREST, UUID.randomUUID(), "알림");
        entityManager.persist(notification);
        entityManager.flush();
        entityManager.clear();

        int deleted = userRepository.deleteUsers(List.of(first.getId(), second.getId()));

        assertThat(deleted).isEqualTo(2);
        assertThat(entityManager.find(User.class, first.getId())).isNull();
        assertThat(entityManager.find(User.class, second.getId())).isNull();
        assertThat(entityManager.find(User.class, stay.getId())).isNotNull();
        assertThat(entityManager.find(Notification.class, notification.getId())).isNull();
    }

    @Test
    @DisplayName("탈퇴 시각이 기준 시각과 정확히 같은 사용자는 이번 실행에서 빠진다 — 다음 실행에서 지워진다")
    void givenDeletedAtEqualsThreshold_whenFindDeletedUserIdsBefore_thenExcluded() {
        Instant threshold = Instant.parse("2026-11-03T00:00:00Z");
        User user = userRepository.save(User.create("edge@monew.com", "경계탈퇴", "password1!"));
        ReflectionTestUtils.setField(user, "deletedAt", threshold);
        entityManager.flush();
        entityManager.clear();

        List<UUID> result = userRepository.findDeletedUserIdsBefore(threshold);

        assertThat(result).doesNotContain(user.getId());
    }
}
