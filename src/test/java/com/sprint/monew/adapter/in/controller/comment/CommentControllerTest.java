package com.sprint.monew.adapter.in.controller.comment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.monew.adapter.in.controller.comment.dto.request.CommentUpdateRequest;
import com.sprint.monew.application.comment.provided.CommentUseCase;
import com.sprint.monew.application.comment.provided.dto.CommentResult;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.CommentErrorType;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(CommentController.class)
class CommentControllerTest {

    private static final String USER_ID_HEADER = "Monew-Request-User-ID";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    // 슬라이스 테스트라 서비스 빈이 없어서 유스케이스는 mock으로 대신한다
    @MockitoBean private CommentUseCase commentUseCase;

    private final UUID commentId = UUID.randomUUID();
    private final UUID requesterId = UUID.randomUUID();

    private MockHttpServletRequestBuilder changeContentRequest(String content) throws Exception {
        return patch("/api/comments/{commentId}", commentId)
                .header(USER_ID_HEADER, requesterId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CommentUpdateRequest(content)));
    }

    @Nested
    @DisplayName("PATCH /api/comments/{commentId} 댓글 수정")
    class ChangeContent {

        @Test
        @DisplayName("정상 요청이면 200과 수정된 댓글(CommentDto)을 응답한다")
        void success() throws Exception {
            UUID articleId = UUID.randomUUID();
            CommentResult result =
                    new CommentResult(
                            commentId,
                            articleId,
                            requesterId,
                            "작성자",
                            "수정 후",
                            3L,
                            true,
                            Instant.parse("2026-10-21T00:00:00Z"));
            given(commentUseCase.changeContent(commentId, requesterId, "수정 후")).willReturn(result);

            mockMvc.perform(changeContentRequest("수정 후"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(commentId.toString()))
                    .andExpect(jsonPath("$.articleId").value(articleId.toString()))
                    .andExpect(jsonPath("$.userId").value(requesterId.toString()))
                    .andExpect(jsonPath("$.userNickname").value("작성자"))
                    .andExpect(jsonPath("$.content").value("수정 후"))
                    .andExpect(jsonPath("$.likeCount").value(3))
                    .andExpect(jsonPath("$.likedByMe").value(true))
                    .andExpect(jsonPath("$.createdAt").value("2026-10-21T00:00:00Z"));
        }

        @Test
        @DisplayName("content가 비어 있으면 400과 details.content를 응답하고 유스케이스를 부르지 않는다")
        void blankContent() throws Exception {
            mockMvc.perform(changeContentRequest(""))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                    .andExpect(jsonPath("$.details.content").exists());
            verifyNoInteractions(commentUseCase);
        }

        @Test
        @DisplayName("content가 501자면 400과 details.content를 응답하고 유스케이스를 부르지 않는다")
        void tooLongContent() throws Exception {
            mockMvc.perform(changeContentRequest("가".repeat(501)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                    .andExpect(jsonPath("$.details.content").exists());
            verifyNoInteractions(commentUseCase);
        }

        @Test
        @DisplayName("Monew-Request-User-ID 헤더가 없으면 400을 응답하고 유스케이스를 부르지 않는다")
        void missingUserIdHeader() throws Exception {
            mockMvc.perform(
                            patch("/api/comments/{commentId}", commentId)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            objectMapper.writeValueAsString(
                                                    new CommentUpdateRequest("수정 후"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
            verifyNoInteractions(commentUseCase);
        }

        @Test
        @DisplayName("작성자가 아니면 403과 COMMENT_NOT_OWNER를 응답한다")
        void notOwner() throws Exception {
            given(commentUseCase.changeContent(any(), any(), any()))
                    .willThrow(new MonewException(CommentErrorType.COMMENT_NOT_OWNER));

            mockMvc.perform(changeContentRequest("수정 후"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("COMMENT_NOT_OWNER"));
        }

        @Test
        @DisplayName("없거나 삭제된 댓글이면 404와 COMMENT_NOT_FOUND를 응답한다")
        void notFound() throws Exception {
            given(commentUseCase.changeContent(any(), any(), any()))
                    .willThrow(new MonewException(CommentErrorType.COMMENT_NOT_FOUND));

            mockMvc.perform(changeContentRequest("수정 후"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("COMMENT_NOT_FOUND"));
        }
    }
}
