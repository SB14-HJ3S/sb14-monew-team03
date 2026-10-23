package com.sprint.monew.adapter.out.interest;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sprint.monew.domain.interest.entity.QInterest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class InterestQueryRepositoryImpl implements InterestQueryRepository {
    private final JPAQueryFactory queryFactory;

    @Override
    public List<String> findAllNames() {
        QInterest interest = QInterest.interest;
        return queryFactory.select(interest.name).from(interest).fetch();
    }
}
