package com.sprint.monew.adapter.in.controller.notification;

import com.sprint.monew.adapter.in.controller.notification.dto.response.NotificationDto;
import com.sprint.monew.application.notification.provided.NotificationUseCase;
import com.sprint.monew.application.notification.provided.dto.NotificationQuery;
import com.sprint.monew.global.annotation.RequestId;
import com.sprint.monew.global.annotation.RestWebAdapter;
import com.sprint.monew.global.pagination.PageResult;
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

@RestWebAdapter(path = "/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationUseCase notificationUseCase;

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

    @RequestId
    @ResponseStatus(HttpStatus.OK)
    @PatchMapping("/{notificationId}")
    public void confirmNotification(@PathVariable("notificationId") UUID notificationId) {
        notificationUseCase.markAsRead(notificationId);
    }

    @RequestId
    @ResponseStatus(HttpStatus.OK)
    @PatchMapping
    public void confirmAllNotifications() {
        notificationUseCase.markAllAsRead();
    }
}
