package com.material.auth.dto.business;

import java.math.BigDecimal;

public record TransportLocationReportRequest(
        BigDecimal longitude,
        BigDecimal latitude,
        String remark
) {
}
