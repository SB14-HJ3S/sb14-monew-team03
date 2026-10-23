package com.sprint.monew.adapter.in.controller.comment;

import com.sprint.monew.adapter.in.controller.comment.dto.request.CommentUpdateRequest;
import com.sprint.monew.adapter.in.controller.comment.dto.response.CommentDto;
import com.sprint.monew.adapter.in.controller.comment.dto.response.CommentLikeDto;
import com.sprint.monew.application.comment.provided.CommentUseCase;
import com.sprint.monew.application.comment.provided.dto.CommentResult;
import com.sprint.monew.global.annotation.RequestId;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentUseCase commentUseCase;

    @PatchMapping("/{commentId}")
    public ResponseEntity<CommentDto> changeContent(
            @PathVariable UUID commentId,
            @RequestHeader("Monew-Request-User-ID") UUID requestId,
            @Valid @RequestBody CommentUpdateRequest request) {
        // 댓글 내용은 개인 정보가 섞일 수 있어 로그에 남기지 않는다
        log.info("댓글 수정 요청: commentId={}, requestId={}", commentId, requestId);
        CommentResult result =
                commentUseCase.changeContent(commentId, requestId, request.content());
        log.info("댓글 수정 완료: commentId={}", commentId);
        return ResponseEntity.ok(CommentDto.create(result));
    }

    @RequestId
    @PostMapping("/{commentId}/comment-likes")
    public ResponseEntity<CommentLikeDto> like(@PathVariable UUID commentId) {
        return null;
    }

    @RequestId
    @DeleteMapping("/{commentId}/comment-likes")
    public ResponseEntity<Void> unlike(@PathVariable UUID commentId) {
        return null;
    }
}
