package com.sprint.monew.global.constant;

// 모뉴에서 쓰는 HTTP 헤더 이름
public final class MonewHeaders {

    // 요청 헤더: 요청한 사용자 ID (프론트가 보냄, Swagger 명세)
    public static final String REQUEST_USER_ID = "Monew-Request-User-ID";

    // 응답 헤더: 요청마다 새로 만드는 추적 번호와 요청 IP (요구사항: 로그 관리)
    public static final String TRACE_ID = "Monew-Request-ID";
    public static final String CLIENT_IP = "Monew-Client-IP";

    private MonewHeaders() {}
}
