package com.sprint.monew.adapter.out.comment;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sprint.monew.application.comment.provided.dto.CommentQuery;
import com.sprint.monew.domain.comment.entity.Comment;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CommentQueryRepositoryImpl implements CommentQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Comment> findByQuery(CommentQuery query) {
        return List.of();
    }

    @Override
    public long countByQuery(CommentQuery query) {
        return 0;
    }
}
