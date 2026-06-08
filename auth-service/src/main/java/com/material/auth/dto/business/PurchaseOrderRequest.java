package com.material.auth.dto.business;

public record PurchaseOrderRequest(
        Long supplierId,
        Long materialId,
        String quantity,
        String remark
) {
}
