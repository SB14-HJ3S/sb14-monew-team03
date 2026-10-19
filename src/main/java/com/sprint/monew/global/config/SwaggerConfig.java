package com.sprint.monew.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Swagger 화면(/swagger-ui) 맨 위에 보이는 API 문서 제목·설명·버전
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("MoNew API")
                                .description("MoNew 프로젝트 API 명세서")
                                .version("v1.0.0"));
    }
}
