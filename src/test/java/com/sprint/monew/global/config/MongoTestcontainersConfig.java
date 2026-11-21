package com.sprint.monew.global.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

// 테스트용 MongoDB를 Docker 컨테이너로 띄운다 — MongoDB가 필요한 통합 테스트(@MongoIntegrationTest)에서만 쓰임
// 접속 정보는 @ServiceConnection이 자동으로 연결해 줘서 yaml에 적지 않는다
@TestConfiguration(proxyBeanMethods = false)
public class MongoTestcontainersConfig {

    @Bean
    @ServiceConnection
    MongoDBContainer mongoContainer() {
        return new MongoDBContainer(DockerImageName.parse("mongo:7"));
    }
}
