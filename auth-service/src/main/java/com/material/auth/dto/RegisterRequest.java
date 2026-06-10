package com.material.auth.dto;

import com.material.common.enums.UserType;

import java.math.BigDecimal;

public record RegisterRequest(
        UserType userType,
        String username,
        String password,
        String displayName,
        String contactPhone,
        String address,
        BigDecimal longitude,
        BigDecimal latitude
) {
}
