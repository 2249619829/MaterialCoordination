package com.material.auth.dto.business;

public record AdminDashboardView(
        Long supplierCount,
        Long purchaserCount,
        Long driverCount,
        Long orderCount,
        Long waitingSupplierConfirmCount,
        Long waitingDriverCount,
        Long transportingCount,
        Long completedCount,
        Long abnormalCount,
        Long pendingPushCount,
        Long deadLetterCount
) {
}
