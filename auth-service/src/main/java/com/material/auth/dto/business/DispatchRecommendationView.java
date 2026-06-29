package com.material.auth.dto.business;

import java.math.BigDecimal;

public record DispatchRecommendationView(
        Long driverId,
        String driverName,
        String vehicleNo,
        String vehicleType,
        Boolean online,
        BigDecimal distanceToOriginKm,
        String ratingScore,
        BigDecimal recommendScore,
        String reason,
        Integer rank
) {
}
