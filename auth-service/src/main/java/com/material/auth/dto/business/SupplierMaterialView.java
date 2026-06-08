package com.material.auth.dto.business;

public record SupplierMaterialView(
        Long id,
        String name,
        String category,
        String unit,
        String price,
        String stock,
        String deliveryCycle
) {
}
