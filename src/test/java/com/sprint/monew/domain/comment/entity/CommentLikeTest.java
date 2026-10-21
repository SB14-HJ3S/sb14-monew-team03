package com.sprint.monew.domain.comment.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.sprint.monew.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CommentLikeTest {

    @Test
    @DisplayName("좋아요를 만들면 누른 사용자와 대상 댓글이 들어간다")
    void create() {
        User user = mock(User.class);
        Comment comment = mock(Comment.class);

        CommentLike commentLike = CommentLike.create(user, comment);

        assertThat(commentLike.getUser()).isEqualTo(user);
        assertThat(commentLike.getComment()).isEqualTo(comment);
    }
}
