package com.material.auth.dto.business;

public record OrderTimelineView(
        Long id,
        String orderId,
        String status,
        String action,
        String operatorType,
        Long operatorId,
        String remark,
        String createdAt
) {
}
