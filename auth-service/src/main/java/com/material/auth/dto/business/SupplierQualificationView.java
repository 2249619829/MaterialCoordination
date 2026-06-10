package com.material.auth.dto.business;

import java.math.BigDecimal;
import java.util.List;

public record SupplierQualificationView(
        Long supplierId,
        String companyName,
        String contactName,
        String contactPhone,
        String licenseNo,
        String address,
        BigDecimal longitude,
        BigDecimal latitude,
        String businessLicenseUrl,
        String safetyCertUrl,
        String insuranceCertUrl,
        String auditStatus,
        String auditStatusText,
        String auditRemark,
        Integer qualificationCompletion,
        List<String> riskTags
) {
}
