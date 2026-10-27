package com.sprint.monew.adapter.in.controller.interest;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sprint.monew.application.interest.provided.InterestUseCase;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.InterestErrorType;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InterestController.class)
class InterestControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private InterestUseCase interestUseCase;

    @Test
    @DisplayName("관심사 삭제에 성공하면 204를 응답한다.")
    void givenExistingInterest_whenDelete_thenNoContent() throws Exception {
        UUID interestId = UUID.randomUUID();

        mockMvc.perform(delete("/api/interests/{interestId}", interestId))
                .andExpect(status().isNoContent());

        verify(interestUseCase).delete(interestId);
    }

    @Test
    @DisplayName("존재하지 않는 관심사를 삭제하면 404와 에러 정보를 응답한다.")
    void givenNonExistingInterest_whenDelete_thenNotFound() throws Exception {
        UUID interestId = UUID.randomUUID();
        doThrow(new MonewException(InterestErrorType.INTEREST_NOT_FOUND, Map.of("id", interestId)))
                .when(interestUseCase)
                .delete(interestId);

        mockMvc.perform(delete("/api/interests/{interestId}", interestId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("INTEREST_NOT_FOUND"))
                .andExpect(jsonPath("$.details.id").value(interestId.toString()));

        verify(interestUseCase).delete(interestId);
    }

    @Test
    @DisplayName("관심사 ID가 UUID 형식이 아니면 400을 응답하고 유스케이스를 호출하지 않는다.")
    void givenInvalidInterestId_whenDelete_thenBadRequest() throws Exception {
        mockMvc.perform(delete("/api/interests/{interestId}", "invalid-id"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.details.interestId").exists());

        verifyNoInteractions(interestUseCase);
    }
}
