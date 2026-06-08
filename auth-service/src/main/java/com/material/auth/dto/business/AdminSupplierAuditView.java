package com.material.auth.dto.business;

public record AdminSupplierAuditView(
        Long supplierId,
        String companyName,
        String contactName,
        String contactPhone,
        String licenseNo,
        String address,
        String ratingScore,
        Integer status,
        String auditStatus,
        Long materialCount,
        Long stockQuantity
) {
}
