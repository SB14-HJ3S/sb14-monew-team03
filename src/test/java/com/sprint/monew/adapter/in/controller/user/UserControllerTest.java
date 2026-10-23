package com.sprint.monew.adapter.in.controller.user;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.monew.adapter.in.controller.user.dto.request.UserRegisterRequest;
import com.sprint.monew.application.user.provided.UserUseCase;
import com.sprint.monew.application.user.provided.dto.UserResult;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.UserErrorType;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private UserUseCase userUseCase;

    private final UUID userId = UUID.randomUUID();
    private final Instant createdAt = Instant.parse("2026-10-21T00:00:00Z");

    private UserResult userResult(String email, String nickname) {
        return new UserResult(userId, email, nickname, createdAt);
    }

    private MockHttpServletRequestBuilder registerRequest(
            String email, String nickname, String password) throws Exception {
        return post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                        objectMapper.writeValueAsString(
                                new UserRegisterRequest(email, nickname, password)));
    }

    @Test
    @DisplayName("회원가입에 성공하면 201과 사용자 정보를 응답하고, 비밀번호는 응답에 없다")
    void givenValidRequest_whenRegister_thenCreated() throws Exception {
        given(userUseCase.register("new@monew.com", "새사람", "password1!"))
                .willReturn(userResult("new@monew.com", "새사람"));

        mockMvc.perform(registerRequest("new@monew.com", "새사람", "password1!"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.email").value("new@monew.com"))
                .andExpect(jsonPath("$.nickname").value("새사람"))
                .andExpect(jsonPath("$.createdAt").value("2026-10-21T00:00:00Z"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @DisplayName("이메일 형식이 아니면 400과 details.email을 응답하고 유스케이스를 부르지 않는다")
    void givenInvalidEmail_whenRegister_thenBadRequest() throws Exception {
        mockMvc.perform(registerRequest("not-email", "새사람", "password1!"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.details.email").exists());
        verifyNoInteractions(userUseCase);
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"password1", "password1 ", "password1가"})
    @DisplayName("비밀번호에 특수문자가 없으면 400과 details.password를 응답하고 유스케이스를 부르지 않는다")
    void givenPasswordWithoutSpecialChar_whenRegister_thenBadRequest(String password)
            throws Exception {
        mockMvc.perform(registerRequest("new@monew.com", "새사람", password))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.details.password").exists());
        verifyNoInteractions(userUseCase);
    }

    static Stream<Arguments> outOfRangeRegisterRequests() {
        String longEmail = "a".repeat(60) + "@" + ("b".repeat(60) + ".").repeat(4) + "com";
        return Stream.of(
                Arguments.of(longEmail, "새사람", "password1!", "email"),
                Arguments.of("new@monew.com", "가".repeat(21), "password1!", "nickname"),
                Arguments.of("new@monew.com", "새사람", "pw12!", "password"),
                Arguments.of("new@monew.com", "새사람", "password1!" + "a".repeat(11), "password"));
    }

    @ParameterizedTest(name = "{3} 길이가 범위를 벗어남")
    @MethodSource("outOfRangeRegisterRequests")
    @DisplayName("길이가 범위를 벗어나면 400과 그 칸의 details를 응답하고 유스케이스를 부르지 않는다")
    void givenOutOfRangeLength_whenRegister_thenBadRequest(
            String email, String nickname, String password, String field) throws Exception {
        mockMvc.perform(registerRequest(email, nickname, password))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details." + field).exists());
        verifyNoInteractions(userUseCase);
    }

    @Test
    @DisplayName("이미 있는 이메일이면 409와 USER_EMAIL_DUPLICATED를 응답한다")
    void givenDuplicatedEmail_whenRegister_thenConflict() throws Exception {
        given(userUseCase.register("dup@monew.com", "중복", "password1!"))
                .willThrow(new MonewException(UserErrorType.USER_EMAIL_DUPLICATED));

        mockMvc.perform(registerRequest("dup@monew.com", "중복", "password1!"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USER_EMAIL_DUPLICATED"));
    }
}
