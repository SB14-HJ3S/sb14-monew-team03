package com.sprint.monew.global.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UrlPathHelper;

/**
 * 서버 내부 정보를 외부에 숨기는 문지기 필터.
 *
 * <p>왜 필요한가: 모니터링(Prometheus)용 주소 /actuator/prometheus에는 서버 메모리·요청 수·DB 연결 수 같은 내부 정보가 나온다. 우리 서버는
 * 인터넷에 공개돼 있어서, 이 필터가 없으면 누구나 주소창에 쳐서 이 정보를 볼 수 있다(공격자에게 힌트가 됨).
 *
 * <p>하는 일:
 *
 * <ul>
 *   <li>/actuator/health — 누구나 통과 (로드밸런서가 "서버 살아 있나" 확인하는 주소)
 *   <li>나머지 /actuator — 같은 서버 안에서 온 요청(Prometheus)만 통과, 외부 요청은 404로 "없는 주소"처럼 숨김
 * </ul>
 *
 * <p>좋은 점: 모니터링은 그대로 쓰면서, 내부 정보는 바깥에서 안 보인다.
 */
@Component
public class ActuatorAccessFilter extends OncePerRequestFilter {

    private static final String ACTUATOR_PATH = "/actuator";
    private static final String HEALTH_PATH = "/actuator/health";

    // 주소를 살짝 바꿔(/%61ctuator 등) 검사를 피하는 걸 막으려고, 주소를 정리한 뒤 판단한다
    private static final UrlPathHelper PATH_HELPER = new UrlPathHelper();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = StringUtils.cleanPath(PATH_HELPER.getPathWithinApplication(request));
        boolean actuator = path.equals(ACTUATOR_PATH) || path.startsWith(ACTUATOR_PATH + "/");
        boolean health = path.equals(HEALTH_PATH) || path.startsWith(HEALTH_PATH + "/");
        return !actuator || health;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!isInternal(request.getRemoteAddr())) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isInternal(String remoteAddr) {
        try {
            InetAddress address = InetAddress.getByName(remoteAddr);
            return address.isLoopbackAddress()
                    || address.isSiteLocalAddress()
                    || address.isLinkLocalAddress();
        } catch (UnknownHostException e) {
            return false;
        }
    }
}
