package com.sprint.monew.adapter.in.controller.comment;

import com.sprint.monew.adapter.in.controller.comment.dto.request.CommentRegisterRequest;
import com.sprint.monew.adapter.in.controller.comment.dto.request.CommentUpdateRequest;
import com.sprint.monew.adapter.in.controller.comment.dto.response.CommentDto;
import com.sprint.monew.adapter.in.controller.comment.dto.response.CommentLikeDto;
import com.sprint.monew.application.comment.provided.CommentUseCase;
import com.sprint.monew.application.comment.provided.dto.CommentLikeResult;
import com.sprint.monew.application.comment.provided.dto.CommentQuery;
import com.sprint.monew.application.comment.provided.dto.CommentResult;
import com.sprint.monew.global.annotation.RequestId;
import com.sprint.monew.global.pagination.PageResult;
import com.sprint.monew.global.pagination.SortDirection;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentUseCase commentUseCase;

    // 댓글 등록만 요청자 ID를 헤더가 아니라 본문(userId)으로 받아서 @RequestId를 붙이지 않는다
    @PostMapping
    public ResponseEntity<CommentDto> register(@Valid @RequestBody CommentRegisterRequest request) {
        // 댓글 내용은 개인 정보가 섞일 수 있어 로그에 남기지 않는다
        log.info("댓글 등록 요청: articleId={}, userId={}", request.articleId(), request.userId());
        CommentResult result =
                commentUseCase.register(request.userId(), request.articleId(), request.content());
        log.info("댓글 등록 완료: commentId={}, articleId={}", result.id(), result.articleId());
        return ResponseEntity.status(HttpStatus.CREATED).body(CommentDto.from(result));
    }

    @RequestId
    @GetMapping
    public ResponseEntity<PageResult<CommentDto>> findByQuery(
            @RequestParam(name = "articleId", required = false) UUID articleId,
            @RequestParam(name = "orderBy") CommentQuery.SortOrder orderBy,
            @RequestParam(name = "direction") SortDirection direction,
            @RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "after", required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant after,
            @RequestParam(name = "limit") @Min(1) int limit) {
        log.info(
                "댓글 목록 조회 요청: articleId={}, orderBy={}, direction={}, limit={}",
                articleId,
                orderBy,
                direction,
                limit);
        CommentQuery query = CommentQuery.of(articleId, orderBy, direction, cursor, after, limit);
        PageResult<CommentDto> result = commentUseCase.findByQuery(query).map(CommentDto::from);
        log.info("댓글 목록 조회 완료: size={}, hasNext={}", result.size(), result.hasNext());
        return ResponseEntity.ok(result);
    }

    @RequestId
    @PatchMapping("/{commentId}")
    public ResponseEntity<CommentDto> changeContent(
            @PathVariable UUID commentId, @Valid @RequestBody CommentUpdateRequest request) {
        // 댓글 내용은 개인 정보가 섞일 수 있어 로그에 남기지 않는다
        log.info("댓글 수정 요청: commentId={}", commentId);
        CommentResult result = commentUseCase.changeContent(commentId, request.content());
        log.info("댓글 수정 완료: commentId={}", commentId);
        return ResponseEntity.ok(CommentDto.from(result));
    }

    @RequestId
    @PostMapping("/{commentId}/comment-likes")
    public ResponseEntity<CommentLikeDto> like(@PathVariable UUID commentId) {
        log.info("댓글 좋아요 요청: commentId={}", commentId);
        CommentLikeResult result = commentUseCase.like(commentId);
        log.info("댓글 좋아요 완료: commentId={}, commentLikeId={}", commentId, result.id());
        return ResponseEntity.ok(CommentLikeDto.from(result));
    }

    @RequestId
    @DeleteMapping("/{commentId}/comment-likes")
    public ResponseEntity<Void> unlike(@PathVariable UUID commentId) {
        log.info("댓글 좋아요 취소 요청: commentId={}", commentId);
        commentUseCase.unlike(commentId);
        log.info("댓글 좋아요 취소 완료: commentId={}", commentId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> softDelete(@PathVariable UUID commentId) {
        log.info("댓글 논리 삭제 요청: commentId={}", commentId);
        commentUseCase.softDeleteComment(commentId);
        log.info("댓글 논리 삭제 완료: commentId={}", commentId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{commentId}/hard")
    public ResponseEntity<Void> hardDelete(@PathVariable UUID commentId) {
        log.info("댓글 물리 삭제 요청: commentId={}", commentId);
        commentUseCase.hardDeleteComment(commentId);
        log.info("댓글 물리 삭제 완료: commentId={}", commentId);
        return ResponseEntity.noContent().build();
    }
}
