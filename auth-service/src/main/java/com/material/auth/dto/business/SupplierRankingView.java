package com.material.auth.dto.business;

public record SupplierRankingView(
        Long supplierId,
        String companyName,
        String ratingScore,
        Integer rank
) {
}
