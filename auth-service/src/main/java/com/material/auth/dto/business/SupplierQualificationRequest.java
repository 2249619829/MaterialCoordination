package com.material.auth.dto.business;

import java.math.BigDecimal;

public record SupplierQualificationRequest(
        String companyName,
        String contactName,
        String contactPhone,
        String licenseNo,
        String address,
        BigDecimal longitude,
        BigDecimal latitude,
        String businessLicenseUrl,
        String safetyCertUrl,
        String insuranceCertUrl
) {
}
