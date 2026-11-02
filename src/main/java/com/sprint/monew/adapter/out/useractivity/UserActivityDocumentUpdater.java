package com.sprint.monew.adapter.out.useractivity;

import static com.sprint.monew.adapter.out.useractivity.document.UserActivityDocument.RECENT_LIMIT;

import com.sprint.monew.adapter.out.useractivity.document.ArticleViewItem;
import com.sprint.monew.adapter.out.useractivity.document.CommentItem;
import com.sprint.monew.adapter.out.useractivity.document.CommentLikeItem;
import com.sprint.monew.adapter.out.useractivity.document.UserActivityDocument;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserActivityDocumentUpdater {
    private final MongoTemplate mongoTemplate;

    public void pushComment(UUID userId, CommentItem item) {
        pushRecent(userId, "comments", item);
    }

    public void pushCommentLike(UUID userId, CommentLikeItem item) {
        pushRecent(userId, "commentLikes", item);
    }

    public void pushArticleView(UUID userId, ArticleViewItem item) {
        pushRecent(userId, "articleViews", item);
    }

    private void pushRecent(UUID userId, String field, Object item) {
        Query query = Query.query(Criteria.where("_id").is(userId));
        Update update = new Update().push(field).atPosition(0).slice(RECENT_LIMIT).each(item);
        mongoTemplate.updateFirst(query, update, UserActivityDocument.class);
    }
}
