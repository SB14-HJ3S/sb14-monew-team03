package com.sprint.monew.adapter.out.comment;

import com.sprint.monew.domain.comment.entity.Comment;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentJpaRepository extends JpaRepository<Comment, UUID> {

    // 동시에 누르면 숫자가 덮어써져서 사라질 수 있어서, DB에서 바로 +1 한다
    // flush: 쿼리 전에 아직 안 보낸 저장을 먼저 보낸다 / clear: 쿼리 후 옛날 숫자를 들고 있는 엔티티를 비운다
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Comment c SET c.likeCount = c.likeCount + 1 WHERE c.id = :id")
    int increaseLikeCount(@Param("id") UUID id);

    // DB에서 바로 -1 한다. 0보다 작아지지는 않는다
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            "UPDATE Comment c SET c.likeCount = c.likeCount - 1"
                    + " WHERE c.id = :id AND c.likeCount > 0")
    int decreaseLikeCount(@Param("id") UUID id);
}
