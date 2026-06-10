package com.material.auth.dto.business;

public record OrderAcceptanceRequest(
        String signerName,
        String acceptanceResult,
        String proofUrl,
        String remark
) {
}
