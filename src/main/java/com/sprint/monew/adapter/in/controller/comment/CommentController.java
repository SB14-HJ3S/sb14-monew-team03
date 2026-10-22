package com.sprint.monew.adapter.in.controller.comment;

import com.sprint.monew.adapter.in.controller.comment.dto.request.CommentUpdateRequest;
import com.sprint.monew.adapter.in.controller.comment.dto.response.CommentDto;
import com.sprint.monew.application.comment.provided.CommentUseCase;
import com.sprint.monew.application.comment.provided.dto.CommentResult;
import com.sprint.monew.global.annotation.RequestId;
import com.sprint.monew.global.context.RequestIdContext;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentUseCase commentUseCase;

    @RequestId
    @PatchMapping("/{commentId}")
    public ResponseEntity<CommentDto> changeContent(
            @PathVariable UUID commentId, @Valid @RequestBody CommentUpdateRequest request) {
        UUID requestUserId = RequestIdContext.get();
        // 댓글 내용은 개인 정보가 섞일 수 있어 로그에 남기지 않는다
        log.info("댓글 수정 요청: commentId={}, requestUserId={}", commentId, requestUserId);
        CommentResult result =
                commentUseCase.changeContent(commentId, requestUserId, request.content());
        log.info("댓글 수정 완료: commentId={}", commentId);
        return ResponseEntity.ok(CommentDto.create(result));
    }
}
