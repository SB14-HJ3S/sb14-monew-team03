package com.sprint.monew.adapter.out.interest;

import com.sprint.monew.global.config.JpaAuditingConfig;
import com.sprint.monew.global.config.QueryDslConfig;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/** Interest 저장소 테스트가 H2와 JPA 설정을 공유하며 테스트마다 롤백하도록 구성한다. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@DataJpaTest(
        showSql = false,
        properties = {"spring.sql.init.mode=never", "spring.jpa.hibernate.ddl-auto=create-drop"})
@AutoConfigureTestDatabase(
        connection = EmbeddedDatabaseConnection.H2,
        replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Import({
    InterestRepositoryAdapter.class,
    SubscriptionRepositoryAdapter.class,
    QueryDslConfig.class,
    JpaAuditingConfig.class
})
@interface InterestJpaTest {}
