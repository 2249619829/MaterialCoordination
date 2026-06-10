package com.material.auth.dto.business;

import java.util.List;

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
        String auditStatusCode,
        String auditRemark,
        Integer qualificationCompletion,
        List<String> riskTags,
        Long materialCount,
        Long stockQuantity
) {
}
