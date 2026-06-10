package com.material.auth.dto.business;

import java.math.BigDecimal;

public record OrderPaymentRequest(
        BigDecimal amount,
        String paymentMethod,
        String paymentReference,
        String proofUrl,
        String remark
) {
}
