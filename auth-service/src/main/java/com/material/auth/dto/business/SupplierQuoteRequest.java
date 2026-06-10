package com.material.auth.dto.business;

import java.math.BigDecimal;

public record SupplierQuoteRequest(
        Long rfqId,
        Long supplierMaterialId,
        BigDecimal unitPrice,
        Integer availableQuantity,
        Integer deliveryDays,
        String remark
) {
}
