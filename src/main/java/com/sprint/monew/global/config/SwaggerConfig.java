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
    private static final String API_DESCRIPTION =
            """
            여러 뉴스 API를 통합하여 사용자에게 맞춤형 뉴스를 제공하고, 의견을 나눌 수 있는 소셜 기능을 갖춘 서비스 **모뉴(MoNew)** 의 API 문서입니다.

            ### 주요 기능
            - **사용자 관리**: 회원가입, 로그인, 닉네임 수정, 탈퇴(논리·물리 삭제)
            - **관심사 관리**: 관심사·키워드 등록, 구독·구독 취소
            - **뉴스 기사 관리**: 네이버 뉴스 API와 한국경제·조선일보·연합뉴스 RSS에서 수집한 기사 조회
            - **댓글 관리**: 기사 댓글 작성·수정·삭제, 좋아요
            - **알림 관리**: 구독한 관심사의 새 기사, 내 댓글 좋아요 알림
            - **사용자 활동 내역**: 구독·댓글·좋아요·최근 본 기사 모아 보기

            ### 공통 규칙
            | 항목 | 내용 |
            |---|---|
            | 요청자 확인 | 요청자가 필요한 API는 `Monew-Request-User-ID` 헤더에 사용자 ID(UUID)를 넣어 주세요 |
            | 목록 조회 | 커서 기반 페이지네이션 (`cursor`, `after`, `limit`) |
            | 에러 응답 | 모든 에러는 `ErrorResponse` 형식 (`timestamp`, `code`, `message`, `details`, `exceptionType`, `status`) |
            """;

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("MoNew API · SB14 Team03")
                                .description(API_DESCRIPTION)
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
