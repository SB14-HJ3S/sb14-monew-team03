package com.sprint.monew.global.web;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.server.PathContainer;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * 프론트 화면 주소로 새로고침·직접 접속해도 404가 안 나게 해 주는 컨트롤러 (#67).
 *
 * <p>왜 필요한가: /articles 같은 화면 이동은 프론트(React)가 브라우저 안에서 처리하는데, 그 주소로 새로고침하면 브라우저가 서버에 요청을 보낸다. 서버에는
 * 그런 주소가 없어서, 이 클래스가 없으면 404 화면이 뜬다.
 *
 * <ul>
 *   <li>화면 주소(점이 없는 GET 요청) → index.html을 돌려줘서 React가 맞는 화면을 그림
 *   <li>/api/** 중 없는 API → index.html 대신 404(메서드·형식이 틀리면 405/415/406)로 응답해서, API 호출이 HTML로 성공한 것처럼
 *       보이지 않게 함
 * </ul>
 */
@Controller
public class SpaForwardController {

    private final ObjectProvider<RequestMappingHandlerMapping> handlerMapping;

    public SpaForwardController(
            @Qualifier("requestMappingHandlerMapping")
                    ObjectProvider<RequestMappingHandlerMapping> handlerMapping) {
        this.handlerMapping = handlerMapping;
    }

    @GetMapping({"/", "/{path:[^\\.]*}", "/**/{path:[^\\.]*}"})
    public String forward(HttpServletRequest request) throws Exception {
        String path = pathWithinApplication(request);
        if (path.equals("/api") || path.startsWith("/api/")) {
            // GET도 POST만 있는 API 주소로 오면 405여야 해서 같은 판별을 거친다
            rejectApiRequest(request);
        }
        return "forward:/index.html";
    }

    // 실제 API가 없는 /api/** 요청은 메서드와 관계없이 이 매핑이 받는다
    @RequestMapping({"/api", "/api/**"})
    public void apiNotFound(HttpServletRequest request) throws Exception {
        rejectApiRequest(request);
    }

    // 같은 주소의 API 매핑을 찾아 원래 Spring MVC가 냈을 응답으로 돌려준다
    // - 메서드가 다르면 405, 메서드는 맞는데 Content-Type이 안 맞으면 415, 응답 형식이 안 맞으면 406
    // - 같은 주소의 API가 없으면 404
    private void rejectApiRequest(HttpServletRequest request) throws Exception {
        PathContainer requestPath = PathContainer.parsePath(pathWithinApplication(request));
        HttpMethod requestMethod = HttpMethod.valueOf(request.getMethod());
        Set<String> allowed = new TreeSet<>();
        for (var entry : handlerMapping.getObject().getHandlerMethods().entrySet()) {
            RequestMappingInfo info = entry.getKey();
            if (entry.getValue().getBeanType() == SpaForwardController.class
                    || info.getPathPatternsCondition() == null
                    || info.getPathPatternsCondition().getPatterns().stream()
                            .noneMatch(pattern -> pattern.matches(requestPath))) {
                continue;
            }
            var methods = info.getMethodsCondition().getMethods();
            if (!methods.isEmpty()
                    && methods.stream().noneMatch(m -> m.asHttpMethod() == requestMethod)) {
                methods.forEach(m -> allowed.add(m.name()));
                continue;
            }
            if (info.getConsumesCondition().getMatchingCondition(request) == null) {
                throw new HttpMediaTypeNotSupportedException(
                        contentTypeOf(request),
                        new ArrayList<>(info.getConsumesCondition().getConsumableMediaTypes()),
                        requestMethod);
            }
            if (info.getProducesCondition().getMatchingCondition(request) == null) {
                throw new HttpMediaTypeNotAcceptableException(
                        new ArrayList<>(info.getProducesCondition().getProducibleMediaTypes()));
            }
        }
        if (!allowed.isEmpty()) {
            throw new HttpRequestMethodNotSupportedException(request.getMethod(), allowed);
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    // Content-Type 형식이 잘못됐으면(예: "abc") 500 대신 415로 응답한다
    private MediaType contentTypeOf(HttpServletRequest request) {
        String contentType = request.getContentType();
        if (!StringUtils.hasText(contentType)) {
            return null;
        }
        try {
            return MediaType.parseMediaType(contentType);
        } catch (InvalidMediaTypeException e) {
            return null;
        }
    }

    // API 매핑 패턴은 context path(앞주소)를 뺀 경로 기준이라 똑같이 빼고 비교한다
    private String pathWithinApplication(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }
}
