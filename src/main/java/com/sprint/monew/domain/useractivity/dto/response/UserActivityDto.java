package com.sprint.monew.domain.useractivity.dto.response;

import com.sprint.monew.domain.article.dto.response.ArticleViewDto;
import com.sprint.monew.domain.interest.dto.response.SubscriptionDto;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserActivityDto(
        UUID id, // 사용자 ID
        String email, // 이메일
        String nickname, // 닉네임
        Instant createdAt, // 가입한 날짜
        List<SubscriptionDto> subscriptions, // 구독 정보
        List<CommentActivityDto> comments, // 최근 작성한 댓글 (최대 10건)
        List<CommentLikeActivityDto> commentLikes, // 최근 좋아요를 누른 댓글 (최대 10건)
        List<ArticleViewDto> articleViews) {} // 최근 본 기사 (최대 10건)
