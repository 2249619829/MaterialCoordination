package com.material.auth.dto.business;

import java.math.BigDecimal;

public record TransportLocationReportView(
        Long id,
        String orderId,
        Long driverId,
        BigDecimal longitude,
        BigDecimal latitude,
        String remark,
        String createdAt
) {
}
