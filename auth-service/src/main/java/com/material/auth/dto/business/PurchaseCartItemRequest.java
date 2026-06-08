package com.material.auth.dto.business;

public record PurchaseCartItemRequest(
        Long materialId,
        String quantity
) {
}
