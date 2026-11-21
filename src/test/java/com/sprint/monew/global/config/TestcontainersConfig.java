package com.sprint.monew.global.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

// 테스트용 PostgreSQL을 Docker 컨테이너로 띄운다 — 모든 통합 테스트(@IntegrationTest)에서 쓰임
// 접속 정보는 @ServiceConnection이 자동으로 연결해 줘서 yaml에 적지 않는다
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:16"));
    }
}
