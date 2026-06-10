package com.material.auth.service.geo;

import java.math.BigDecimal;

public record Coordinates(
        BigDecimal longitude,
        BigDecimal latitude
) {
}
