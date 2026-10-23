package com.sprint.monew.domain.comment.event;

import java.util.UUID;

// 댓글에 좋아요가 등록됐을 때 발행
public record CommentLikedEvent(
        UUID commentId, // 좋아요가 눌린 댓글
        UUID commentUserId, // 댓글 작성자
        UUID likedById, // 좋아요 누른 사용자
        String likedByNickname) {} // 알림 문구에 들어갈 닉네임
