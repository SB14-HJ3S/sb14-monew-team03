package com.sprint.monew.adapter.out.comment;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sprint.monew.adapter.out.article.ArticleRepositoryAdapter;
import com.sprint.monew.adapter.out.user.UserRepositoryAdapter;
import com.sprint.monew.domain.article.entity.Article;
import com.sprint.monew.domain.comment.entity.Comment;
import com.sprint.monew.domain.comment.entity.CommentLike;
import com.sprint.monew.domain.user.entity.User;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.CommentErrorType;
import com.sprint.monew.global.type.ArticleSourceType;
import com.sprint.monew.support.IntegrationTest;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class CommentLikeRepositoryAdapterTest {

    @Autowired private CommentLikeRepositoryAdapter commentLikeRepository;
    @Autowired private CommentRepositoryAdapter commentRepository;
    @Autowired private UserRepositoryAdapter userRepository;
    @Autowired private ArticleRepositoryAdapter articleRepository;

    @Test
    @DisplayName("같은 사용자가 같은 댓글에 좋아요를 또 저장하면 COMMENT_LIKE_DUPLICATED 예외가 난다")
    void givenAlreadyLiked_whenSave_thenDuplicated() {
        User user = userRepository.save(User.create("like@monew.com", "좋아요", "password1!"));
        Article article =
                articleRepository.save(
                        Article.create(
                                ArticleSourceType.NAVER,
                                "https://news.example.com/1",
                                "기사 제목",
                                LocalDateTime.now(),
                                "기사 요약"));
        Comment comment = commentRepository.save(Comment.create(user, article, "댓글 내용"));
        commentLikeRepository.save(CommentLike.create(user, comment));

        assertThatThrownBy(() -> commentLikeRepository.save(CommentLike.create(user, comment)))
                .isInstanceOf(MonewException.class)
                .extracting("errorType")
                .isEqualTo(CommentErrorType.COMMENT_LIKE_DUPLICATED);
    }
}
