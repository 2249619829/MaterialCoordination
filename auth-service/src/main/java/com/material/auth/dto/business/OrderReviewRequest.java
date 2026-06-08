package com.material.auth.dto.business;

public record OrderReviewRequest(
        String targetType,
        Long targetId,
        Integer score,
        String content
) {
}
