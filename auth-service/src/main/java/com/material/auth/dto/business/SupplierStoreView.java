package com.material.auth.dto.business;

import java.util.List;

public record SupplierStoreView(
        SupplierCatalogView supplier,
        List<PurchaseOrderView> recentOrders,
        List<OrderReviewView> recentReviews,
        Integer totalOrders,
        Integer availableMaterials,
        String serviceSummary
) {
}
