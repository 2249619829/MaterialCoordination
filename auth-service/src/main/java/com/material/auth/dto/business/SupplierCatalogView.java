package com.material.auth.dto.business;

import java.util.List;

public record SupplierCatalogView(
        Long id,
        String companyName,
        String contactName,
        String region,
        String address,
        String rating,
        List<String> certifications,
        List<SupplierMaterialView> materials
) {
}
