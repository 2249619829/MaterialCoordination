package com.material.auth.dto.business;

import java.math.BigDecimal;

public record SupplierMaterialManageRequest(
        Long materialId,
        String materialName,
        String category,
        String unit,
        BigDecimal supplyPrice,
        Integer stockQuantity,
        Integer dailyCapacity,
        BigDecimal deliveryRadiusKm,
        Integer status
) {
}
