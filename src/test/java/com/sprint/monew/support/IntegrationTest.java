package com.sprint.monew.support;

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
 * 통합 테스트 클래스에 붙이는 어노테이션 — 아래 3개를 한 번에 붙여 준다.
 *
 * <ul>
 *   <li>{@code @SpringBootTest}: 스프링 앱 전체를 띄워서 테스트
 *   <li>{@code @ActiveProfiles("test")}: 테스트 설정(application-test.yaml) 사용
 *   <li>{@code @Import(TestcontainersConfig.class)}: 테스트용 PostgreSQL 컨테이너 띄우기
 * </ul>
 *
 * <p>사용: 테스트 클래스 위에 {@code @IntegrationTest}만 붙이면 된다. MongoDB도 필요하면 {@link MongoIntegrationTest}를
 * 쓴다.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
public @interface IntegrationTest {}
