package com.sprint.monew.adapter.out.useractivity;

import static com.sprint.monew.adapter.out.useractivity.document.UserActivityDocument.FIELD_ARTICLE_VIEWS;
import static com.sprint.monew.adapter.out.useractivity.document.UserActivityDocument.FIELD_COMMENTS;
import static com.sprint.monew.adapter.out.useractivity.document.UserActivityDocument.FIELD_COMMENT_LIKES;
import static com.sprint.monew.adapter.out.useractivity.document.UserActivityDocument.FIELD_SUBSCRIPTIONS;

import com.sprint.monew.adapter.out.useractivity.document.ArticleViewItem;
import com.sprint.monew.adapter.out.useractivity.document.CommentItem;
import com.sprint.monew.adapter.out.useractivity.document.CommentLikeItem;
import com.sprint.monew.adapter.out.useractivity.document.SubscriptionItem;
import com.sprint.monew.adapter.out.useractivity.document.UserActivityDocument;
import com.sprint.monew.application.useractivity.provided.dto.ArticleViewActivityResult;
import com.sprint.monew.application.useractivity.provided.dto.CommentActivityResult;
import com.sprint.monew.application.useractivity.provided.dto.CommentLikeActivityResult;
import com.sprint.monew.application.useractivity.provided.dto.SubscriptionActivityResult;
import com.sprint.monew.application.useractivity.required.UserActivityWriter;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

/**
 * 활동 내역 문서를 MongoTemplate 부분 업데이트로 갱신한다. - 한 사람 문서: _id로 찾아 updateFirst - 여러 사람 문서(댓글·관심사·기사 변경,
 * 사용자 완전 삭제): 항목 ID로 찾아 updateMulti (#56 인덱스 사용)
 */
@Repository
@RequiredArgsConstructor
public class UserActivityMongoWriter implements UserActivityWriter {

    private static final Class<UserActivityDocument> DOC = UserActivityDocument.class;

    private final MongoTemplate mongoTemplate;
    private final UserActivityDocumentUpdater updater;

    // 사용자

    @Override
    public void create(UUID userId, String email, String nickname, Instant createdAt) {
        // 없을 때만 빈 문서를 만든다. 이미 있으면 아무것도 바꾸지 않는다 (가입 이벤트가 두 번 와도 안전)
        Update update =
                new Update()
                        .setOnInsert("email", email)
                        .setOnInsert("nickname", nickname)
                        .setOnInsert("createdAt", createdAt)
                        .setOnInsert(FIELD_SUBSCRIPTIONS, List.of())
                        .setOnInsert(FIELD_COMMENTS, List.of())
                        .setOnInsert(FIELD_COMMENT_LIKES, List.of())
                        .setOnInsert(FIELD_ARTICLE_VIEWS, List.of());
        mongoTemplate.upsert(byUser(userId), update, DOC);
    }

    @Override
    public void updateNickname(UUID userId, String nickname) {
        // 화면(프로필 카드)에 보이는 본인 닉네임만 갱신한다.
        // 다른 사람 문서의 "좋아요한 댓글" 작성자 닉네임은 화면에 표시되지 않아 좋아요 시점 값으로 둔다.
        mongoTemplate.updateFirst(byUser(userId), new Update().set("nickname", nickname), DOC);
    }

    @Override
    public void deleteAll(List<UUID> userIds) {
        mongoTemplate.remove(Query.query(Criteria.where("_id").in(userIds)), DOC);
    }

    @Override
    public void removeCommentLikesOfCommentUsers(List<UUID> commentUserIds) {
        // 완전 삭제된 사용자의 댓글은 DB에서 CASCADE로 지워지므로, 다른 사람 "좋아요한 댓글"에서도 뺀다
        mongoTemplate.updateMulti(
                Query.query(
                        Criteria.where(FIELD_COMMENT_LIKES + ".commentUserId").in(commentUserIds)),
                new Update()
                        .pull(
                                FIELD_COMMENT_LIKES,
                                Query.query(Criteria.where("commentUserId").in(commentUserIds))),
                DOC);
    }

    // 구독 관심사

    @Override
    public void addSubscription(UUID userId, SubscriptionActivityResult item) {
        // 같은 관심사가 이미 있으면 빼고 맨 앞에 다시 넣는다 (중복 방지)
        pullFromUser(userId, FIELD_SUBSCRIPTIONS, "interestId", item.interestId());
        mongoTemplate.updateFirst(
                byUser(userId),
                new Update()
                        .push(FIELD_SUBSCRIPTIONS)
                        .atPosition(0)
                        .each(SubscriptionItem.from(item)),
                DOC);
    }

    @Override
    public void removeSubscription(UUID userId, UUID interestId) {
        pullFromUser(userId, FIELD_SUBSCRIPTIONS, "interestId", interestId);
    }

    @Override
    public void updateInterestKeywords(UUID interestId, List<String> keywords) {
        mongoTemplate.updateMulti(
                Query.query(Criteria.where(FIELD_SUBSCRIPTIONS + ".interestId").is(interestId)),
                new Update()
                        .set(FIELD_SUBSCRIPTIONS + ".$[sub].interestKeywords", keywords)
                        .filterArray(Criteria.where("sub.interestId").is(interestId)),
                DOC);
    }

    @Override
    public void removeInterest(UUID interestId) {
        pullFromAll(FIELD_SUBSCRIPTIONS, "interestId", interestId);
    }

    // 댓글

    @Override
    public void addComment(UUID userId, CommentActivityResult item) {
        updater.pushComment(userId, CommentItem.from(item));
    }

    @Override
    public void updateCommentContent(UUID commentId, String content) {
        // 쓴 사람 문서의 댓글 내용
        mongoTemplate.updateMulti(
                Query.query(Criteria.where(FIELD_COMMENTS + ".commentId").is(commentId)),
                new Update()
                        .set(FIELD_COMMENTS + ".$[c].content", content)
                        .filterArray(Criteria.where("c.commentId").is(commentId)),
                DOC);
        // 좋아요 누른 사람 문서의 댓글 내용 ("좋아요한 댓글" 화면에 내용이 보임)
        mongoTemplate.updateMulti(
                Query.query(Criteria.where(FIELD_COMMENT_LIKES + ".commentId").is(commentId)),
                new Update()
                        .set(FIELD_COMMENT_LIKES + ".$[l].commentContent", content)
                        .filterArray(Criteria.where("l.commentId").is(commentId)),
                DOC);
    }

    @Override
    public void removeComment(UUID commentId) {
        pullFromAll(FIELD_COMMENTS, "commentId", commentId);
        pullFromAll(FIELD_COMMENT_LIKES, "commentId", commentId);
    }

    // 좋아요

    @Override
    public void addCommentLike(UUID userId, CommentLikeActivityResult item) {
        updater.pushCommentLike(userId, CommentLikeItem.from(item));
    }

    @Override
    public void removeCommentLike(UUID userId, UUID commentId) {
        pullFromUser(userId, FIELD_COMMENT_LIKES, "commentId", commentId);
    }

    @Override
    public void updateCommentLikeCount(UUID commentId, long likeCount) {
        // 쓴 사람 문서 "최근 작성한 댓글"
        mongoTemplate.updateMulti(
                Query.query(Criteria.where(FIELD_COMMENTS + ".commentId").is(commentId)),
                new Update()
                        .set(FIELD_COMMENTS + ".$[c].likeCount", likeCount)
                        .filterArray(Criteria.where("c.commentId").is(commentId)),
                DOC);
        // 좋아요 누른 사람들 문서 "좋아요한 댓글"
        mongoTemplate.updateMulti(
                Query.query(Criteria.where(FIELD_COMMENT_LIKES + ".commentId").is(commentId)),
                new Update()
                        .set(FIELD_COMMENT_LIKES + ".$[l].commentLikeCount", likeCount)
                        .filterArray(Criteria.where("l.commentId").is(commentId)),
                DOC);
    }

    // 기사

    @Override
    public void addArticleView(UUID userId, ArticleViewActivityResult item) {
        // 다시 본 기사는 기존 항목을 빼고 맨 앞에 넣어 "마지막으로 본 순"을 유지한다
        pullFromUser(userId, FIELD_ARTICLE_VIEWS, "articleId", item.articleId());
        updater.pushArticleView(userId, ArticleViewItem.from(item));
    }

    @Override
    public void removeArticle(UUID articleId) {
        pullFromAll(FIELD_ARTICLE_VIEWS, "articleId", articleId);
        pullFromAll(FIELD_COMMENTS, "articleId", articleId);
        pullFromAll(FIELD_COMMENT_LIKES, "articleId", articleId);
    }

    // ===== 도우미 =====

    private Query byUser(UUID userId) {
        return Query.query(Criteria.where("_id").is(userId));
    }

    /** 한 사람 문서의 배열에서 조건에 맞는 항목을 뺀다 */
    private void pullFromUser(UUID userId, String arrayField, String itemField, UUID targetId) {
        mongoTemplate.updateFirst(
                byUser(userId),
                new Update().pull(arrayField, Query.query(Criteria.where(itemField).is(targetId))),
                DOC);
    }

    /** 해당 항목이 들어 있는 모든 문서의 배열에서 그 항목을 뺀다 */
    private void pullFromAll(String arrayField, String itemField, UUID targetId) {
        mongoTemplate.updateMulti(
                Query.query(Criteria.where(arrayField + "." + itemField).is(targetId)),
                new Update().pull(arrayField, Query.query(Criteria.where(itemField).is(targetId))),
                DOC);
    }
}
