package com.sprint.monew.global.config;

import com.sprint.monew.global.annotation.RequestId;
import com.sprint.monew.global.constant.MonewHeaders;
import com.sprint.monew.global.exception.ErrorResponse;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Swagger 화면(/swagger-ui) 맨 위에 보이는 API 문서 제목·설명·버전
@Configuration
public class SwaggerConfig {

    private static final String ERROR_RESPONSE = "ErrorResponse";
    private static final String ERROR_RESPONSE_REF = "#/components/schemas/" + ERROR_RESPONSE;

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

    // 모든 API에 500 응답을 넣고, 4xx·5xx 응답 본문은 ErrorResponse 형식으로 표시한다
    @Bean
    public OpenApiCustomizer errorResponseCustomizer() {
        return openApi -> {
            openApi.getComponents().addSchemas(ERROR_RESPONSE, errorResponseSchema());
            openApi.getPaths().values().stream()
                    .flatMap(path -> path.readOperations().stream())
                    .forEach(operation -> addErrorResponses(operation.getResponses()));
        };
    }

    private Schema<?> errorResponseSchema() {
        return ModelConverters.getInstance()
                .resolveAsResolvedSchema(new AnnotatedType(ErrorResponse.class))
                .schema;
    }

    private void addErrorResponses(ApiResponses responses) {
        responses.computeIfAbsent("500", code -> new ApiResponse().description("서버 내부 오류"));

        Schema<?> errorSchemaRef = new Schema<>().$ref(ERROR_RESPONSE_REF);
        Content errorContent =
                new Content().addMediaType("*/*", new MediaType().schema(errorSchemaRef));
        responses.forEach(
                (code, response) -> {
                    if (code.startsWith("4") || code.startsWith("5")) {
                        response.setContent(errorContent);
                    }
                });
    }
}
