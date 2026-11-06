package com.sprint.monew.adapter.in.controller.notification;

import com.sprint.monew.adapter.in.controller.notification.dto.response.NotificationDto;
import com.sprint.monew.application.notification.provided.NotificationUseCase;
import com.sprint.monew.application.notification.provided.dto.NotificationQuery;
import com.sprint.monew.global.annotation.RequestId;
import com.sprint.monew.global.annotation.RestWebAdapter;
import com.sprint.monew.global.pagination.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

@Tag(name = "알림 관리", description = "알림 관련 API")
@RestWebAdapter(path = "/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationUseCase notificationUseCase;

    @Operation(summary = "알림 목록 조회", description = "확인하지 않은 알림을 최신순으로 커서 페이지네이션 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(
                responseCode = "400",
                description = "잘못된 요청 (커서·after 짝 불일치, 커서 형식 오류, limit 범위 오류)"),
        @ApiResponse(responseCode = "404", description = "사용자 정보 없음")
    })
    @RequestId
    @ResponseStatus(HttpStatus.OK)
    @GetMapping
    public PageResult<NotificationDto> findUnreadNotifications(
            @RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "after", required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant after,
            @RequestParam(name = "limit", required = true)
                    @Min(value = 1, message = "limit은 1이상이어야 합니다.")
                    int limit) {
        NotificationQuery query = NotificationQuery.of(cursor, after, limit);

        return notificationUseCase.findByQuery(query).map(NotificationDto::from);
    }

    @Operation(summary = "알림 확인", description = "알림을 확인 처리합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "알림 확인 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 (알림 ID 형식 오류)"),
        @ApiResponse(responseCode = "404", description = "사용자 정보 또는 알림 정보 없음")
    })
    @RequestId
    @ResponseStatus(HttpStatus.OK)
    @PatchMapping("/{notificationId}")
    public void confirmNotification(@PathVariable("notificationId") UUID notificationId) {
        notificationUseCase.markAsRead(notificationId);
    }

    @Operation(summary = "전체 알림 확인", description = "확인하지 않은 알림을 한 번에 확인 처리합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "전체 알림 확인 성공"),
        @ApiResponse(responseCode = "404", description = "사용자 정보 없음")
    })
    @RequestId
    @ResponseStatus(HttpStatus.OK)
    @PatchMapping
    public void confirmAllNotifications() {
        notificationUseCase.markAllAsRead();
    }
}
