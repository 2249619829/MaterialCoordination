package com.material.auth.dto.business;

import java.math.BigDecimal;

public record RfqQuoteView(
        Long id,
        Long rfqId,
        Long supplierId,
        String supplierName,
        Long supplierMaterialId,
        Long materialId,
        String materialName,
        String category,
        String unit,
        BigDecimal unitPrice,
        Integer availableQuantity,
        Integer deliveryDays,
        String remark,
        BigDecimal recommendScore,
        String status,
        String createdAt
) {
}
