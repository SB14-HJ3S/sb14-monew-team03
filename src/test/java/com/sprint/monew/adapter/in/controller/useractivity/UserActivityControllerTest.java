package com.sprint.monew.adapter.in.controller.useractivity;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sprint.monew.application.useractivity.provided.UserActivityUseCase;
import com.sprint.monew.application.useractivity.provided.dto.CommentActivityResult;
import com.sprint.monew.application.useractivity.provided.dto.UserActivityResult;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.UserErrorType;
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

@WebMvcTest(UserActivityController.class)
class UserActivityControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private UserActivityUseCase userActivityUseCase;

    @Test
    @DisplayName("활동 내역 조회에 성공하면 200과 활동 내역을 응답한다.")
    void givenExistingUser_whenFind_thenOk() throws Exception {
        UUID userId = UUID.randomUUID();
        CommentActivityResult comment =
                new CommentActivityResult(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "기사 제목",
                        userId,
                        "우디",
                        "댓글 내용",
                        3L,
                        Instant.parse("2026-10-30T00:00:00Z"));
        when(userActivityUseCase.findByUserId(userId))
                .thenReturn(
                        new UserActivityResult(
                                userId,
                                "woody@monew.com",
                                "우디",
                                Instant.parse("2026-10-01T00:00:00Z"),
                                List.of(),
                                List.of(comment),
                                List.of(),
                                List.of()));

        mockMvc.perform(get("/api/user-activities/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.nickname").value("우디"))
                .andExpect(jsonPath("$.comments[0].articleTitle").value("기사 제목"))
                .andExpect(jsonPath("$.comments[0].likeCount").value(3))
                .andExpect(jsonPath("$.subscriptions").isEmpty());

        verify(userActivityUseCase).findByUserId(userId);
    }

    @Test
    @DisplayName("없거나 탈퇴한 사용자면 404와 에러 정보를 응답한다.")
    void givenUnknownUser_whenFind_thenNotFound() throws Exception {
        UUID userId = UUID.randomUUID();
        when(userActivityUseCase.findByUserId(userId))
                .thenThrow(
                        new MonewException(UserErrorType.USER_NOT_FOUND, Map.of("userId", userId)));

        mockMvc.perform(get("/api/user-activities/{userId}", userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
                .andExpect(jsonPath("$.details.userId").value(userId.toString()));
    }

    @Test
    @DisplayName("사용자 ID가 UUID 형식이 아니면 400을 응답하고 유스케이스를 호출하지 않는다.")
    void givenInvalidUserId_whenFind_thenBadRequest() throws Exception {
        mockMvc.perform(get("/api/user-activities/{userId}", "invalid-id"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        verifyNoInteractions(userActivityUseCase);
    }
}
