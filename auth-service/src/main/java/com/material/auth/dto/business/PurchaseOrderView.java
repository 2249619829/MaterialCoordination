package com.material.auth.dto.business;

import java.math.BigDecimal;

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
        String createdAt,
        String acceptanceStatus,
        String acceptanceSummary,
        String acceptanceProofUrl,
        String paymentStatus,
        String paymentSummary,
        String paymentExpiresAt,
        String paymentProofUrl,
        String originAddress,
        BigDecimal originLongitude,
        BigDecimal originLatitude,
        String destinationAddress,
        BigDecimal destinationLongitude,
        BigDecimal destinationLatitude
) {
}
