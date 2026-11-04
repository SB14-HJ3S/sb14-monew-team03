package com.sprint.monew.application.useractivity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sprint.monew.application.useractivity.provided.dto.CommentActivityResult;
import com.sprint.monew.application.useractivity.provided.dto.UserActivityResult;
import com.sprint.monew.application.useractivity.required.UserActivityReader;
import com.sprint.monew.application.useractivity.required.UserActivityWriter;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.UserErrorType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserActivityApplicationTest {

    @Mock private UserActivityReader userActivityReader;
    @Mock private UserActivityWriter writer;

    @InjectMocks private UserActivityApplication userActivityApplication;

    @Test
    @DisplayName("활동 내역이 있으면 그대로 반환한다.")
    void givenExistingUser_whenFind_thenReturnsActivity() {
        UUID userId = UUID.randomUUID();
        UserActivityResult activity =
                new UserActivityResult(
                        userId,
                        "woody@monew.com",
                        "우디",
                        Instant.now(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of());
        when(userActivityReader.findByUserId(userId)).thenReturn(Optional.of(activity));

        UserActivityResult result = userActivityApplication.findByUserId(userId);

        assertThat(result).isEqualTo(activity);
    }

    @Test
    @DisplayName("없거나 탈퇴한 사용자면 USER_NOT_FOUND 예외를 던진다.")
    void givenUnknownUser_whenFind_thenThrowsUserNotFound() {
        UUID userId = UUID.randomUUID();
        when(userActivityReader.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userActivityApplication.findByUserId(userId))
                .isInstanceOf(MonewException.class)
                .extracting("errorType")
                .isEqualTo(UserErrorType.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("사용자 완전 삭제는 문서 삭제 후 다른 사람 문서의 그 사람들 댓글 좋아요까지 정리한다.")
    void whenUsersHardDeleted_thenDeleteDocumentsAndCleanLikes() {
        List<UUID> userIds = List.of(UUID.randomUUID(), UUID.randomUUID());

        userActivityApplication.usersHardDeleted(userIds);

        InOrder order = inOrder(writer);
        order.verify(writer).deleteAll(userIds);
        order.verify(writer).removeCommentLikesOfCommentUsers(userIds);
    }

    @Test
    @DisplayName("댓글 작성은 작성자 문서에 댓글을 추가한다.")
    void whenCommentCreated_thenAddComment() {
        UUID userId = UUID.randomUUID();
        CommentActivityResult item =
                new CommentActivityResult(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "기사",
                        userId,
                        "우디",
                        "내용",
                        0L,
                        Instant.now());

        userActivityApplication.commentCreated(userId, item);

        verify(writer).addComment(userId, item);
    }
}
