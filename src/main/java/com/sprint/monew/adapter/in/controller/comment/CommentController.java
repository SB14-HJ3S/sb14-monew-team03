package com.sprint.monew.adapter.in.controller.comment;

import com.sprint.monew.adapter.in.controller.comment.dto.request.CommentRegisterRequest;
import com.sprint.monew.adapter.in.controller.comment.dto.request.CommentUpdateRequest;
import com.sprint.monew.adapter.in.controller.comment.dto.response.CommentDto;
import com.sprint.monew.adapter.in.controller.comment.dto.response.CommentLikeDto;
import com.sprint.monew.adapter.in.controller.comment.dto.response.CursorPageResponseCommentDto;
import com.sprint.monew.application.comment.provided.CommentUseCase;
import com.sprint.monew.application.comment.provided.dto.CommentLikeResult;
import com.sprint.monew.application.comment.provided.dto.CommentQuery;
import com.sprint.monew.application.comment.provided.dto.CommentResult;
import com.sprint.monew.global.annotation.RequestId;
import com.sprint.monew.global.pagination.PageResult;
import com.sprint.monew.global.pagination.SortDirection;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "댓글 관리", description = "댓글 관련 API")
@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentUseCase commentUseCase;

    // 댓글 등록만 요청자 ID를 헤더가 아니라 본문(userId)으로 받아서 @RequestId를 붙이지 않는다
    @Operation(summary = "댓글 등록", description = "기사에 댓글을 등록합니다. 등록하면 기사의 댓글 수가 1 늘어납니다.")
    @ApiResponse(responseCode = "201", description = "등록 성공")
    @ApiResponse(responseCode = "400", description = "입력값 검증 실패 (필수값 누락, 내용이 비었거나 500자 초과)")
    @ApiResponse(responseCode = "404", description = "없거나 탈퇴한 사용자, 없거나 삭제된 기사")
    @PostMapping
    public ResponseEntity<CommentDto> register(@Valid @RequestBody CommentRegisterRequest request) {
        // 댓글 내용은 개인 정보가 섞일 수 있어 로그에 남기지 않는다
        log.info("댓글 등록 요청: articleId={}, userId={}", request.articleId(), request.userId());
        CommentResult result =
                commentUseCase.register(request.userId(), request.articleId(), request.content());
        log.info("댓글 등록 완료: commentId={}, articleId={}", result.id(), result.articleId());
        return ResponseEntity.status(HttpStatus.CREATED).body(CommentDto.from(result));
    }

    @Operation(
            summary = "댓글 목록 조회",
            description = "삭제되지 않은 댓글을 작성 시각 또는 좋아요 수로 정렬해 커서 페이지네이션으로 조회합니다.")
    @ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content =
                    @Content(schema = @Schema(implementation = CursorPageResponseCommentDto.class)))
    @ApiResponse(
            responseCode = "400",
            description =
                    "요청자 ID 누락·형식 오류, 정렬 값 오류, limit 1 미만, cursor 형식 오류, cursor·after 중 하나만 보냄")
    @ApiResponse(responseCode = "404", description = "cursor에 해당하는 댓글 없음")
    @RequestId
    @GetMapping
    public ResponseEntity<PageResult<CommentDto>> findByQuery(
            @Parameter(description = "기사 ID (없으면 기사 구분 없이 조회)")
                    @RequestParam(name = "articleId", required = false)
                    UUID articleId,
            @Parameter(
                            description = "정렬 기준 (createdAt: 작성 시각, likeCount: 좋아요 수)",
                            schema =
                                    @Schema(
                                            type = "string",
                                            allowableValues = {"createdAt", "likeCount"}))
                    @RequestParam(name = "orderBy")
                    CommentQuery.SortOrder orderBy,
            @Parameter(description = "정렬 방향 (ASC, DESC)") @RequestParam(name = "direction")
                    SortDirection direction,
            @Parameter(description = "이전 응답의 nextCursor (첫 페이지면 비움, after와 함께 보냄)")
                    @RequestParam(name = "cursor", required = false)
                    String cursor,
            @Parameter(description = "이전 응답의 nextAfter (첫 페이지면 비움, cursor와 함께 보냄)")
                    @RequestParam(name = "after", required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant after,
            @Parameter(description = "한 번에 조회할 댓글 수 (1 이상)") @RequestParam(name = "limit") @Min(1)
                    int limit) {
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

    @Operation(summary = "댓글 내용 수정", description = "본인이 작성한 댓글의 내용을 수정합니다.")
    @ApiResponse(responseCode = "200", description = "수정 성공")
    @ApiResponse(
            responseCode = "400",
            description = "내용이 비었거나 500자 초과, 요청자 ID 누락·형식 오류, 댓글 ID 형식 오류")
    @ApiResponse(responseCode = "403", description = "본인이 작성한 댓글이 아님")
    @ApiResponse(responseCode = "404", description = "없거나 삭제된 댓글")
    @RequestId
    @PatchMapping("/{commentId}")
    public ResponseEntity<CommentDto> changeContent(
            @Parameter(description = "댓글 ID") @PathVariable UUID commentId,
            @Valid @RequestBody CommentUpdateRequest request) {
        // 댓글 내용은 개인 정보가 섞일 수 있어 로그에 남기지 않는다
        log.info("댓글 수정 요청: commentId={}", commentId);
        CommentResult result = commentUseCase.changeContent(commentId, request.content());
        log.info("댓글 수정 완료: commentId={}", commentId);
        return ResponseEntity.ok(CommentDto.from(result));
    }

    @Operation(summary = "댓글 좋아요", description = "댓글에 좋아요를 누릅니다. 댓글의 좋아요 수가 1 늘어납니다.")
    @ApiResponse(responseCode = "200", description = "좋아요 성공")
    @ApiResponse(responseCode = "400", description = "요청자 ID 누락·형식 오류, 댓글 ID 형식 오류")
    @ApiResponse(responseCode = "404", description = "없거나 삭제된 댓글, 없거나 탈퇴한 사용자")
    @ApiResponse(responseCode = "409", description = "이미 좋아요를 누른 댓글")
    @RequestId
    @PostMapping("/{commentId}/comment-likes")
    public ResponseEntity<CommentLikeDto> like(
            @Parameter(description = "댓글 ID") @PathVariable UUID commentId) {
        log.info("댓글 좋아요 요청: commentId={}", commentId);
        CommentLikeResult result = commentUseCase.like(commentId);
        log.info("댓글 좋아요 완료: commentId={}, commentLikeId={}", commentId, result.id());
        return ResponseEntity.ok(CommentLikeDto.from(result));
    }

    @Operation(summary = "댓글 좋아요 취소", description = "누른 좋아요를 취소합니다. 댓글의 좋아요 수가 1 줄어듭니다.")
    @ApiResponse(responseCode = "200", description = "좋아요 취소 성공")
    @ApiResponse(responseCode = "400", description = "요청자 ID 누락·형식 오류, 댓글 ID 형식 오류")
    @ApiResponse(responseCode = "404", description = "없거나 삭제된 댓글, 좋아요를 누른 적 없음")
    @RequestId
    @DeleteMapping("/{commentId}/comment-likes")
    public ResponseEntity<Void> unlike(
            @Parameter(description = "댓글 ID") @PathVariable UUID commentId) {
        log.info("댓글 좋아요 취소 요청: commentId={}", commentId);
        commentUseCase.unlike(commentId);
        log.info("댓글 좋아요 취소 완료: commentId={}", commentId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "댓글 논리 삭제", description = "댓글에 삭제 표시를 합니다. 기사의 댓글 수가 1 줄어듭니다.")
    @ApiResponse(responseCode = "204", description = "삭제 성공")
    @ApiResponse(responseCode = "400", description = "댓글 ID 형식 오류")
    @ApiResponse(responseCode = "404", description = "없거나 이미 삭제된 댓글")
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> softDelete(
            @Parameter(description = "댓글 ID") @PathVariable UUID commentId) {
        log.info("댓글 논리 삭제 요청: commentId={}", commentId);
        commentUseCase.softDeleteComment(commentId);
        log.info("댓글 논리 삭제 완료: commentId={}", commentId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "댓글 물리 삭제",
            description = "댓글과 좋아요, 좋아요 알림을 함께 지웁니다. 논리 삭제 전이던 댓글이면 기사의 댓글 수가 1 줄어듭니다.")
    @ApiResponse(responseCode = "204", description = "삭제 성공")
    @ApiResponse(responseCode = "400", description = "댓글 ID 형식 오류")
    @ApiResponse(responseCode = "404", description = "없는 댓글")
    @DeleteMapping("/{commentId}/hard")
    public ResponseEntity<Void> hardDelete(
            @Parameter(description = "댓글 ID") @PathVariable UUID commentId) {
        log.info("댓글 물리 삭제 요청: commentId={}", commentId);
        commentUseCase.hardDeleteComment(commentId);
        log.info("댓글 물리 삭제 완료: commentId={}", commentId);
        return ResponseEntity.noContent().build();
    }
}
