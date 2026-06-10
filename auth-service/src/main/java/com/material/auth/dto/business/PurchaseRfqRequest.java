package com.material.auth.dto.business;

import java.math.BigDecimal;

public record PurchaseRfqRequest(
        String materialName,
        String category,
        String unit,
        String quantity,
        String deliveryAddress,
        BigDecimal longitude,
        BigDecimal latitude,
        String remark
) {
}
