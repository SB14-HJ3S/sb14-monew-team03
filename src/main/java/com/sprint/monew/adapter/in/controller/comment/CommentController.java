package com.sprint.monew.adapter.in.controller.comment;

import com.sprint.monew.adapter.in.controller.comment.dto.request.CommentUpdateRequest;
import com.sprint.monew.adapter.in.controller.comment.dto.response.CommentDto;
import com.sprint.monew.application.comment.provided.CommentUseCase;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentUseCase commentUseCase;

    @PatchMapping("/{commentId}")
    public ResponseEntity<CommentDto> changeContent(
            @PathVariable UUID commentId,
            @RequestHeader("Monew-Request-User-ID") UUID requesterId,
            @Valid @RequestBody CommentUpdateRequest request) {
        return null;
    }
}
