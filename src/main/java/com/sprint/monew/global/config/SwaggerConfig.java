package com.sprint.monew.global.config;

import com.sprint.monew.global.annotation.RequestId;
import com.sprint.monew.global.constant.MonewHeaders;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import org.springdoc.core.customizers.OperationCustomizer;
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

    // @RequestId가 붙은 API에만 요청자 ID 헤더 입력칸을 필수로 추가한다
    @Bean
    public OperationCustomizer requestUserIdHeaderCustomizer() {
        return (operation, handlerMethod) -> {
            if (handlerMethod.hasMethodAnnotation(RequestId.class)) {
                operation.addParametersItem(
                        new HeaderParameter()
                                .name(MonewHeaders.REQUEST_USER_ID)
                                .description("요청자 ID")
                                .required(true)
                                .schema(new StringSchema().format("uuid")));
            }
            return operation;
        };
    }
}
