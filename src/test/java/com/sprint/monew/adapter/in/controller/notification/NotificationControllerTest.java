package com.sprint.monew.adapter.in.controller.notification;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sprint.monew.application.notification.provided.NotificationUseCase;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.NotificationErrorType;
import com.sprint.monew.global.exception.errortype.UserErrorType;
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
}
