package com.material.auth.dto.business;

public record MaterialOptionView(
        Long id,
        String materialCode,
        String materialName,
        String category,
        String unit
) {
}
