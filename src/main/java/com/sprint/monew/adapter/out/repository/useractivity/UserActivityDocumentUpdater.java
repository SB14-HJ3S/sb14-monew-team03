package com.sprint.monew.adapter.out.repository.useractivity;

import static com.sprint.monew.adapter.out.repository.useractivity.document.UserActivityDocument.FIELD_ARTICLE_VIEWS;
import static com.sprint.monew.adapter.out.repository.useractivity.document.UserActivityDocument.FIELD_COMMENTS;
import static com.sprint.monew.adapter.out.repository.useractivity.document.UserActivityDocument.FIELD_COMMENT_LIKES;
import static com.sprint.monew.adapter.out.repository.useractivity.document.UserActivityDocument.STORED_LIMIT;

import com.mongodb.client.result.UpdateResult;
import com.sprint.monew.adapter.out.repository.useractivity.document.ArticleViewItem;
import com.sprint.monew.adapter.out.repository.useractivity.document.CommentItem;
import com.sprint.monew.adapter.out.repository.useractivity.document.CommentLikeItem;
import com.sprint.monew.adapter.out.repository.useractivity.document.UserActivityDocument;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActivityDocumentUpdater {
    private final MongoTemplate mongoTemplate;

    public void pushComment(UUID userId, CommentItem item) {
        pushRecent(userId, FIELD_COMMENTS, item);
    }

    public void pushCommentLike(UUID userId, CommentLikeItem item) {
        pushRecent(userId, FIELD_COMMENT_LIKES, item);
    }

    public void pushArticleView(UUID userId, ArticleViewItem item) {
        pushRecent(userId, FIELD_ARTICLE_VIEWS, item);
    }

    private void pushRecent(UUID userId, String field, Object item) {
        Query query = Query.query(Criteria.where("_id").is(userId));
        Update update = new Update().push(field).atPosition(0).slice(STORED_LIMIT).each(item);

        UpdateResult result = mongoTemplate.updateFirst(query, update, UserActivityDocument.class);
        if (result.getMatchedCount() == 0) {
            log.warn("활동 내역 문서가 없어 {} 추가를 건너뜀: userId={}", field, userId);
        }
    }
}
