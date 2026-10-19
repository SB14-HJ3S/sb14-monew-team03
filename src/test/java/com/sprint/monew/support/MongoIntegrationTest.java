package com.sprint.monew.support;

import com.sprint.monew.global.config.MongoTestcontainersConfig;
import com.sprint.monew.global.config.TestcontainersConfig;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * MongoDB까지 필요한 통합 테스트에 붙이는 어노테이션 — @IntegrationTest와 같고, MongoDB 컨테이너만 하나 더 띄운다.
 *
 * <p>사용: 활동내역처럼 MongoDB를 쓰는 테스트에만 {@code @MongoIntegrationTest}를 붙인다. 나머지는 {@link IntegrationTest}
 * (컨테이너가 하나 적어서 더 빠름)
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@SpringBootTest
@ActiveProfiles("test")
@Import({TestcontainersConfig.class, MongoTestcontainersConfig.class})
public @interface MongoIntegrationTest {}
