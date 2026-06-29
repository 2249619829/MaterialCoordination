package com.material.auth.service.impl.support;

import com.material.auth.dto.business.DispatchRecommendationView;
import com.material.auth.entity.DriverProfile;
import com.material.auth.entity.PurchaseOrder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public final class DispatchRecommendationSupport {
    private DispatchRecommendationSupport() {
    }

    public static List<DispatchRecommendationView> rank(PurchaseOrder order, List<DriverProfile> drivers, int limit) {
        AtomicInteger rank = new AtomicInteger(1);
        return drivers.stream()
                .filter(driver -> driver.getDriverId() != null)
                .filter(driver -> driver.getLongitude() != null && driver.getLatitude() != null)
                .map(driver -> toRecommendation(order, driver))
                .sorted(Comparator
                        .comparing(DispatchRecommendationView::recommendScore, Comparator.reverseOrder())
                        .thenComparing(DispatchRecommendationView::distanceToOriginKm)
                        .thenComparing(DispatchRecommendationView::driverId))
                .limit(limit)
                .map(item -> new DispatchRecommendationView(
                        item.driverId(),
                        item.driverName(),
                        item.vehicleNo(),
                        item.vehicleType(),
                        item.online(),
                        item.distanceToOriginKm(),
                        item.ratingScore(),
                        item.recommendScore(),
                        item.reason(),
                        rank.getAndIncrement()
                ))
                .toList();
    }

    private static DispatchRecommendationView toRecommendation(PurchaseOrder order, DriverProfile driver) {
        BigDecimal distanceKm = distanceKm(
                order.getOriginLongitude(),
                order.getOriginLatitude(),
                driver.getLongitude(),
                driver.getLatitude()
        );
        BigDecimal rating = driver.getRatingScore() == null ? BigDecimal.ZERO : driver.getRatingScore();
        boolean online = Integer.valueOf(1).equals(driver.getAttendanceStatus());
        BigDecimal locationScore = BigDecimal.valueOf(Math.max(0D, 40D - distanceKm.doubleValue()));
        BigDecimal onlineScore = online ? BigDecimal.valueOf(80L) : BigDecimal.ZERO;
        BigDecimal recommendScore = rating.multiply(BigDecimal.TEN)
                .add(locationScore)
                .add(onlineScore)
                .setScale(2, RoundingMode.HALF_UP);
        String statusText = online ? "在线" : "离线";
        String ratingText = formatRating(rating);
        String reason = statusText
                + " · 距发货地 " + distanceKm.stripTrailingZeros().toPlainString() + " KM"
                + " · 评分 " + ratingText;
        return new DispatchRecommendationView(
                driver.getDriverId(),
                driver.getRealName(),
                driver.getVehicleNo(),
                driver.getVehicleType(),
                online,
                distanceKm,
                ratingText,
                recommendScore,
                reason,
                0
        );
    }

    private static BigDecimal distanceKm(BigDecimal originLongitude,
                                         BigDecimal originLatitude,
                                         BigDecimal driverLongitude,
                                         BigDecimal driverLatitude) {
        double earthRadiusKm = 6371.0088D;
        double originLat = Math.toRadians(originLatitude.doubleValue());
        double driverLat = Math.toRadians(driverLatitude.doubleValue());
        double deltaLat = Math.toRadians(driverLatitude.subtract(originLatitude).doubleValue());
        double deltaLon = Math.toRadians(driverLongitude.subtract(originLongitude).doubleValue());
        double a = Math.sin(deltaLat / 2D) * Math.sin(deltaLat / 2D)
                + Math.cos(originLat) * Math.cos(driverLat)
                * Math.sin(deltaLon / 2D) * Math.sin(deltaLon / 2D);
        double c = 2D * Math.atan2(Math.sqrt(a), Math.sqrt(1D - a));
        return BigDecimal.valueOf(earthRadiusKm * c).setScale(2, RoundingMode.HALF_UP);
    }

    private static String formatRating(BigDecimal ratingScore) {
        return (ratingScore == null ? BigDecimal.ZERO : ratingScore).stripTrailingZeros().toPlainString();
    }
}
