package com.material.auth.dto.business;

public record NearbySupplierView(
        Long supplierId,
        String companyName,
        String address,
        String ratingScore,
        Double distanceKm
) {
}
