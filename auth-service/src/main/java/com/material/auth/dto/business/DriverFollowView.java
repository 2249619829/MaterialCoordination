package com.material.auth.dto.business;

public record DriverFollowView(
        Long purchaserId,
        String purchaserName,
        boolean followedByDriver,
        boolean followedByPurchaser
) {
}
