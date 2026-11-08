package com.sprint.monew.adapter.in.controller.useractivity.dto.response;

import com.sprint.monew.application.useractivity.provided.usecase.dto.UserActivityResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "사용자 활동 내역")
public record UserActivityDto(
        @Schema(description = "사용자 ID") UUID id,
        @Schema(description = "이메일") String email,
        @Schema(description = "닉네임") String nickname,
        @Schema(description = "가입한 날짜") Instant createdAt,
        @Schema(description = "구독 중인 관심사") List<SubscriptionActivityDto> subscriptions,
        @Schema(description = "최근 작성한 댓글 (최대 10건)") List<CommentActivityDto> comments,
        @Schema(description = "최근 좋아요를 누른 댓글 (최대 10건)") List<CommentLikeActivityDto> commentLikes,
        @Schema(description = "최근 본 뉴스 기사 (최대 10건)") List<ArticleViewActivityDto> articleViews) {

    public static UserActivityDto from(UserActivityResult r) {
        return new UserActivityDto(
                r.id(),
                r.email(),
                r.nickname(),
                r.createdAt(),
                r.subscriptions().stream().map(SubscriptionActivityDto::from).toList(),
                r.comments().stream().map(CommentActivityDto::from).toList(),
                r.commentLikes().stream().map(CommentLikeActivityDto::from).toList(),
                r.articleViews().stream().map(ArticleViewActivityDto::from).toList());
    }
}
