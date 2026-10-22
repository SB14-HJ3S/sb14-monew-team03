package com.sprint.monew.global.web;

import com.sprint.monew.global.annotation.RequestId;
import com.sprint.monew.global.context.RequestIdContext;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.CommonErrorType;
import java.lang.reflect.Method;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;

class RequestIdInterceptorTest {

    private static final String REQUEST_ID = "MoNew-Request-User-ID";

    private final RequestIdInterceptor interceptor = new RequestIdInterceptor();

    @AfterEach
    void init() {
        RequestIdContext.clear();
    }

    @Test
    @DisplayName("@RequestId 선언된 핸들러는 context에 requestId를 저장한다.")
    void givenAnnotated_whenHandled_thenSaveRequestId() throws NoSuchMethodException {
        UUID requestId = UUID.randomUUID();
        String strRequestId = requestId.toString();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(REQUEST_ID, strRequestId);

        boolean result = interceptor.preHandle(request, response, handlerMethod("required"));

        Assertions.assertThat(result).isTrue();
        Assertions.assertThat(RequestIdContext.get()).isEqualTo(requestId);
    }

    @Test
    @DisplayName("@RequestId 선언된 핸들러에 request header가 있어야 한다.")
    void givenAnnotated_whenNoHeader_thenThrow() {

        MonewException thrown =
                Assertions.catchThrowableOfType(
                        MonewException.class,
                        () ->
                                interceptor.preHandle(
                                        new MockHttpServletRequest(),
                                        new MockHttpServletResponse(),
                                        handlerMethod("required")));

        Assertions.assertThat(thrown.getErrorType()).isEqualTo(CommonErrorType.INVALID_REQUEST);
    }

    @Test
    @DisplayName("@RequestId 선언된 핸들러에 request header는 유효한 UUID 형식이어야 한다.")
    void givenAnnotated_whenHeaderIsNotUuid_thenThrow() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(REQUEST_ID, "invalid uuid");

        MonewException thrown =
                Assertions.catchThrowableOfType(
                        MonewException.class,
                        () ->
                                interceptor.preHandle(
                                        request,
                                        new MockHttpServletResponse(),
                                        handlerMethod("required")));

        Assertions.assertThat(thrown.getErrorType()).isEqualTo(CommonErrorType.INVALID_REQUEST);
    }

    @Test
    @DisplayName("요청이 끝나면 context는 ThreadLocal 자원을 반환한다.")
    void afterCompletion() throws NoSuchMethodException {
        UUID requestId = UUID.randomUUID();
        RequestIdContext.set(requestId);

        interceptor.afterCompletion(
                new MockHttpServletRequest(),
                new MockHttpServletResponse(),
                handlerMethod("required"),
                null);

        Assertions.assertThat(RequestIdContext.get()).isNull();
    }

    @Test
    @DisplayName("@RequestId 선언 없으면 그냥 통과한다.")
    void givenNotAnnotated_whenHandled_thenReturnTrue() throws NoSuchMethodException {
        boolean result =
                interceptor.preHandle(
                        new MockHttpServletRequest(),
                        new MockHttpServletResponse(),
                        handlerMethod("notRequired"));

        Assertions.assertThat(result).isTrue();
    }

    @Test
    @DisplayName("핸들러 메서드가 아니면 그냥 통과한다.")
    void whenNotHandler_thenReturnTrue() throws NoSuchMethodException {
        boolean result =
                interceptor.preHandle(
                        new MockHttpServletRequest(),
                        new MockHttpServletResponse(),
                        new ResourceHttpRequestHandler());

        Assertions.assertThat(result).isTrue();
    }

    private HandlerMethod handlerMethod(String name) throws NoSuchMethodException {
        Method method = TestController.class.getDeclaredMethod(name);
        return new HandlerMethod(new TestController(), method);
    }

    static class TestController {
        @RequestId
        public void required() {}

        public void notRequired() {}
    }
}
