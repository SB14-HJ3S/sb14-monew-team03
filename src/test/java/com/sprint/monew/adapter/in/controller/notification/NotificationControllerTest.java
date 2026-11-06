package com.sprint.monew.adapter.in.controller.notification;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sprint.monew.application.notification.provided.NotificationUseCase;
import com.sprint.monew.application.notification.provided.dto.NotificationQuery;
import com.sprint.monew.application.notification.provided.dto.NotificationResult;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.NotificationErrorType;
import com.sprint.monew.global.exception.errortype.UserErrorType;
import com.sprint.monew.global.pagination.PageResult;
import com.sprint.monew.global.type.ResourceType;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

    private static final String USER_HEADER = "Monew-Request-User-ID";

    @Autowired private MockMvc mockMvc;

    @MockitoBean private NotificationUseCase notificationUseCase;

    @Test
    @DisplayName("알림 단건 확인에 성공하면 200을 응답한다.")
    void givenNotificationId_whenConfirm_thenOk() throws Exception {
        UUID notificationId = UUID.randomUUID();

        mockMvc.perform(
                        patch("/api/notifications/{notificationId}", notificationId)
                                .header(USER_HEADER, UUID.randomUUID()))
                .andExpect(status().isOk());

        verify(notificationUseCase).markAsRead(notificationId);
    }

    @Test
    @DisplayName("존재하지 않거나 남의 알림을 확인하면 404와 에러 정보를 응답한다.")
    void givenNonExistingNotification_whenConfirm_thenNotFound() throws Exception {
        UUID notificationId = UUID.randomUUID();
        doThrow(
                        new MonewException(
                                NotificationErrorType.NOTIFICATION_NOT_FOUND,
                                Map.of("notificationId", notificationId)))
                .when(notificationUseCase)
                .markAsRead(notificationId);

        mockMvc.perform(
                        patch("/api/notifications/{notificationId}", notificationId)
                                .header(USER_HEADER, UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOTIFICATION_NOT_FOUND"))
                .andExpect(jsonPath("$.details.notificationId").value(notificationId.toString()));

        verify(notificationUseCase).markAsRead(notificationId);
    }

    @Test
    @DisplayName("알림 ID가 UUID 형식이 아니면 400을 응답하고 유스케이스를 호출하지 않는다.")
    void givenInvalidNotificationId_whenConfirm_thenBadRequest() throws Exception {
        mockMvc.perform(
                        patch("/api/notifications/{notificationId}", "invalid-id")
                                .header(USER_HEADER, UUID.randomUUID()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.details.notificationId").exists());

        verifyNoInteractions(notificationUseCase);
    }

    @Test
    @DisplayName("알림 전체 확인에 성공하면 200을 응답한다.")
    void givenUser_whenConfirmAll_thenOk() throws Exception {
        mockMvc.perform(patch("/api/notifications").header(USER_HEADER, UUID.randomUUID()))
                .andExpect(status().isOk());

        verify(notificationUseCase).markAllAsRead();
    }

    @Test
    @DisplayName("존재하지 않는 사용자가 전체 확인하면 404와 에러 정보를 응답한다.")
    void givenNonExistingUser_whenConfirmAll_thenNotFound() throws Exception {
        doThrow(new MonewException(UserErrorType.USER_NOT_FOUND))
                .when(notificationUseCase)
                .markAllAsRead();

        mockMvc.perform(patch("/api/notifications").header(USER_HEADER, UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));

        verify(notificationUseCase).markAllAsRead();
    }

    @Test
    @DisplayName("알림 목록 조회에 성공하면 200과 알림 목록을 응답한다.")
    void givenNotifications_whenFindUnread_thenOk() throws Exception {
        UUID userId = UUID.randomUUID();
        NotificationResult notification =
                new NotificationResult(
                        UUID.randomUUID(),
                        Instant.parse("2026-11-06T00:00:00Z"),
                        null,
                        false,
                        userId,
                        "[축구]와 관련된 기사가 3건 등록되었습니다.",
                        ResourceType.INTEREST,
                        UUID.randomUUID());
        when(notificationUseCase.findByQuery(any(NotificationQuery.class)))
                .thenReturn(new PageResult<>(List.of(notification), null, null, 1, 1L, false));

        mockMvc.perform(get("/api/notifications").header(USER_HEADER, userId).param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].content").value("[축구]와 관련된 기사가 3건 등록되었습니다."))
                .andExpect(jsonPath("$.hasNext").value(false));

        verify(notificationUseCase).findByQuery(any(NotificationQuery.class));
    }

    @Test
    @DisplayName("limit이 1보다 작으면 400을 응답하고 유스케이스를 호출하지 않는다.")
    void givenZeroLimit_whenFindUnread_thenBadRequest() throws Exception {
        mockMvc.perform(
                        get("/api/notifications")
                                .header(USER_HEADER, UUID.randomUUID())
                                .param("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        verifyNoInteractions(notificationUseCase);
    }
}
