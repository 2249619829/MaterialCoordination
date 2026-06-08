package com.material.auth.dto.business;

public record NotificationView(
        String id,
        String title,
        String content,
        String type,
        String status,
        String createTime
) {
}
