package com.material.auth.dto.business;

public record OrderReviewView(
        Long id,
        String orderId,
        String reviewerType,
        Long reviewerId,
        String targetType,
        Long targetId,
        Integer score,
        String content,
        String createTime
) {
}
