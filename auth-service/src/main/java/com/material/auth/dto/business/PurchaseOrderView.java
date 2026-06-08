package com.material.auth.dto.business;

public record PurchaseOrderView(
        String id,
        Long purchaserId,
        String purchaserName,
        Long supplierId,
        String supplierName,
        Long materialId,
        String materialName,
        String category,
        String quantity,
        String amount,
        String status,
        String source,
        String pushedTo,
        Long driverId,
        String pushStatus,
        String createdAt
) {
}
