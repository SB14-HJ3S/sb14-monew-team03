package com.sprint.monew.application.useractivity.required;

import com.sprint.monew.application.useractivity.provided.dto.ArticleViewActivityResult;
import com.sprint.monew.application.useractivity.provided.dto.CommentActivityResult;
import com.sprint.monew.application.useractivity.provided.dto.CommentLikeActivityResult;
import com.sprint.monew.application.useractivity.provided.dto.SubscriptionActivityResult;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** 활동 내역 문서 갱신 포트. MongoDB 어댑터가 구현한다. Application은 Mongo 항목 타입을 모르도록 결과 객체로 주고받는다. */
public interface UserActivityWriter {

    // 사용자

    /** 가입 직후 빈 문서. 이미 있으면 아무것도 바꾸지 않는다 */
    void create(UUID userId, String email, String nickname, Instant createdAt);

    /** 본인 문서의 닉네임 */
    void updateNickname(UUID userId, String nickname);

    /** 완전 삭제된 사용자들의 문서 삭제 */
    void deleteAll(List<UUID> userIds);

    /** 완전 삭제된 사용자들이 쓴 댓글을 다른 사람 "좋아요한 댓글"에서 제거 */
    void removeCommentLikesOfCommentUsers(List<UUID> commentUserIds);

    // 구독 관심사

    void addSubscription(UUID userId, SubscriptionActivityResult item);

    void removeSubscription(UUID userId, UUID interestId);

    /** 그 관심사를 구독한 모든 문서의 키워드 */
    void updateInterestKeywords(UUID interestId, List<String> keywords);

    /** 모든 문서의 구독 목록에서 그 관심사 제거 */
    void removeInterest(UUID interestId);

    // 댓글

    void addComment(UUID userId, CommentActivityResult item);

    /** 쓴 사람 문서 + 좋아요 누른 사람 문서의 댓글 내용 */
    void updateCommentContent(UUID commentId, String content);

    /** 쓴 사람 문서 + 좋아요 누른 사람 문서에서 그 댓글 제거 */
    void removeComment(UUID commentId);

    // 좋아요

    void addCommentLike(UUID userId, CommentLikeActivityResult item);

    void removeCommentLike(UUID userId, UUID commentId);

    void updateCommentLikeCount(UUID commentId, long likeCount);

    // 기사

    /** 이미 본 기사면 맨 앞으로 올린다 */
    void addArticleView(UUID userId, ArticleViewActivityResult item);

    /** 모든 문서의 본 기사·댓글·좋아요한 댓글에서 그 기사 항목 제거 */
    void removeArticle(UUID articleId);
}
