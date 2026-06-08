package com.material.auth.dto.business;

public record DeadLetterStatsView(
        String queueName,
        Integer messages,
        Integer consumers
) {
}
