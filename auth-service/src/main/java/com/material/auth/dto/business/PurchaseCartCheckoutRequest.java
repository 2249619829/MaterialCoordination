package com.material.auth.dto.business;

import java.util.List;

public record PurchaseCartCheckoutRequest(
        Long supplierId,
        List<PurchaseCartItemRequest> items,
        String remark
) {
}
