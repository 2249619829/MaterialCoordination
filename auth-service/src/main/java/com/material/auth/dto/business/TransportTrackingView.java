package com.material.auth.dto.business;

import java.math.BigDecimal;
import java.util.List;

public record TransportTrackingView(
        String orderId,
        String status,
        Long driverId,
        String originAddress,
        BigDecimal originLongitude,
        BigDecimal originLatitude,
        String destinationAddress,
        BigDecimal destinationLongitude,
        BigDecimal destinationLatitude,
        List<OrderTimelineView> timeline
) {
}
