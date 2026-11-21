package com.sprint.monew.global.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// QueryDSL용 JPAQueryFactory 빈 — Repository에서 주입받아 동적 쿼리(검색·정렬·커서 페이지네이션)를 쓴다
@Configuration
public class QueryDslConfig {

    @Bean
    public JPAQueryFactory jpaQueryFactory(EntityManager em) {
        return new JPAQueryFactory(em);
    }
}
