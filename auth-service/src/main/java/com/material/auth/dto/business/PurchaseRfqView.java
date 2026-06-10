package com.material.auth.dto.business;

import java.math.BigDecimal;

public record PurchaseRfqView(
        Long id,
        Long purchaserId,
        String purchaserName,
        String materialName,
        String category,
        String unit,
        String quantity,
        String deliveryAddress,
        BigDecimal longitude,
        BigDecimal latitude,
        String status,
        Integer quoteCount,
        RfqQuoteView bestQuote,
        String remark,
        String createdAt
) {
}
