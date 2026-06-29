package com.material.auth.service.impl.support;

import com.material.auth.dto.business.FulfillmentRankingsView;
import com.material.auth.dto.business.ParticipantRankingView;
import com.material.auth.dto.business.SupplierRankingView;
import com.material.auth.entity.DriverProfile;
import com.material.auth.entity.OrderReview;
import com.material.auth.entity.PurchaserProfile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class FulfillmentRankingSupport {
    private static final String TARGET_PURCHASER = "PURCHASER";
    private static final String TARGET_DRIVER = "DRIVER";

    private FulfillmentRankingSupport() {
    }

    public static FulfillmentRankingsView create(List<OrderReview> reviews,
                                                 List<PurchaserProfile> purchasers,
                                                 List<SupplierRankingView> suppliers,
                                                 List<DriverProfile> drivers) {
        return new FulfillmentRankingsView(
                purchaserRankings(reviews, purchasers),
                supplierRankings(suppliers),
                driverRankings(reviews, drivers)
        );
    }

    private static List<ParticipantRankingView> purchaserRankings(List<OrderReview> reviews,
                                                                  List<PurchaserProfile> purchasers) {
        Map<Long, BigDecimal> scoresById = averageReviewScores(reviews, TARGET_PURCHASER);
        if (scoresById.isEmpty()) {
            return List.of();
        }
        Map<Long, PurchaserProfile> profiles = purchasers.stream()
                .collect(Collectors.toMap(PurchaserProfile::getPurchaserId, Function.identity(), (left, right) -> left));
        return scoresById.entrySet().stream()
                .sorted(Map.Entry.<Long, BigDecimal>comparingByValue().reversed())
                .limit(10)
                .collect(ArrayList::new, (ranking, entry) -> {
                    PurchaserProfile profile = profiles.get(entry.getKey());
                    if (profile != null) {
                        ranking.add(new ParticipantRankingView(
                                entry.getKey(),
                                profile.getCompanyName(),
                                formatRating(entry.getValue()),
                                ranking.size() + 1
                        ));
                    }
                }, ArrayList::addAll);
    }

    private static List<ParticipantRankingView> supplierRankings(List<SupplierRankingView> suppliers) {
        return suppliers.stream()
                .map(item -> new ParticipantRankingView(
                        item.supplierId(),
                        item.companyName(),
                        item.ratingScore(),
                        item.rank()
                ))
                .toList();
    }

    private static List<ParticipantRankingView> driverRankings(List<OrderReview> reviews, List<DriverProfile> drivers) {
        Map<Long, BigDecimal> reviewedScores = averageReviewScores(reviews, TARGET_DRIVER);
        List<DriverProfile> sortedDrivers = drivers.stream()
                .sorted(Comparator.comparing((DriverProfile profile) -> reviewedScores.getOrDefault(
                        profile.getDriverId(),
                        profile.getRatingScore() == null ? BigDecimal.ZERO : profile.getRatingScore()
                )).reversed())
                .limit(10)
                .toList();
        List<ParticipantRankingView> ranking = new ArrayList<>();
        for (DriverProfile driver : sortedDrivers) {
            BigDecimal score = reviewedScores.getOrDefault(driver.getDriverId(),
                    driver.getRatingScore() == null ? BigDecimal.ZERO : driver.getRatingScore());
            ranking.add(new ParticipantRankingView(
                    driver.getDriverId(),
                    driver.getRealName() + " · " + driver.getVehicleNo(),
                    formatRating(score),
                    ranking.size() + 1
            ));
        }
        return ranking;
    }

    private static Map<Long, BigDecimal> averageReviewScores(List<OrderReview> reviews, String targetType) {
        return reviews.stream()
                .filter(review -> targetType.equals(review.getTargetType()))
                .collect(Collectors.groupingBy(
                        OrderReview::getTargetId,
                        Collectors.collectingAndThen(Collectors.toList(), values -> {
                            BigDecimal total = values.stream()
                                    .map(review -> BigDecimal.valueOf(review.getScore()))
                                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                            return total.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
                        })
                ));
    }

    private static String formatRating(BigDecimal ratingScore) {
        return (ratingScore == null ? BigDecimal.ZERO : ratingScore).stripTrailingZeros().toPlainString();
    }
}
