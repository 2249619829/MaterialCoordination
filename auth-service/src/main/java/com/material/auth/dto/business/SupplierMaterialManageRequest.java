package com.material.auth.dto.business;

import java.math.BigDecimal;

public record SupplierMaterialManageRequest(
        Long materialId,
        BigDecimal supplyPrice,
        Integer stockQuantity,
        Integer dailyCapacity,
        BigDecimal deliveryRadiusKm,
        Integer status
) {
}
