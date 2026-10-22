package com.sprint.monew.global.web;

import com.sprint.monew.global.context.RequestIdContext;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.CommonErrorType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.UUID;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RequestIdInterceptor implements HandlerInterceptor {
    private static final String REQUEST_ID = "MoNew-Request-User-ID";

    @Override
    public boolean preHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler) {

        String header = request.getHeader(REQUEST_ID);
        try {
            UUID requestId = UUID.fromString(header);
            RequestIdContext.set(requestId);
            return true;
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new MonewException(
                    CommonErrorType.INVALID_REQUEST, Collections.singletonMap(REQUEST_ID, header));
        }
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            @Nullable Exception ex) {
        RequestIdContext.clear();
    }
}
