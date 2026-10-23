package com.sprint.monew.global.logging;

import com.sprint.monew.global.constant.MonewHeaders;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

// 요청마다 로그 앞에 [요청 ID] [IP] [사용자 ID]를 붙여, 어떤 요청에서 찍힌 로그인지 찾기 쉽게 한다
// 예) INFO [3f2a…] [203.0.113.7] [9b1c…] … : 댓글 등록 완료
// 사용자 ID는 로그 표시용일 뿐 요청을 막지 않는다 — API에 필요한 사용자 ID는 이 필터가 아니라 각 API가 Swagger대로 헤더로 받는다
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcLoggingFilter extends OncePerRequestFilter {

    public static final String MDC_REQUEST_ID = "requestId";
    public static final String MDC_CLIENT_IP = "clientIp";
    public static final String MDC_USER_ID = "userId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString();
        // 로드밸런서(ALB) 뒤의 실제 IP는 server.forward-headers-strategy=native 설정으로 들어온다
        String clientIp = request.getRemoteAddr();

        MDC.put(MDC_REQUEST_ID, requestId);
        MDC.put(MDC_CLIENT_IP, clientIp);
        UUID userId = parseUserId(request.getHeader(MonewHeaders.REQUEST_USER_ID));
        if (userId != null) {
            MDC.put(MDC_USER_ID, userId.toString());
        }
        response.setHeader(MonewHeaders.TRACE_ID, requestId);
        response.setHeader(MonewHeaders.CLIENT_IP, clientIp);

        try {
            filterChain.doFilter(request, response);
        } finally {
            // 스레드가 재사용돼도 다음 요청에 값이 남지 않도록 이 필터가 넣은 값만 지운다
            MDC.remove(MDC_REQUEST_ID);
            MDC.remove(MDC_CLIENT_IP);
            MDC.remove(MDC_USER_ID);
        }
    }

    // 로그에 엉뚱한 값이 남지 않도록 UUID 형식일 때만 사용자 ID로 본다
    private UUID parseUserId(String header) {
        if (header == null || header.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(header.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
