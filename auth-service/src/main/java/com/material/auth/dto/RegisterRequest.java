package com.material.auth.dto;

import com.material.common.enums.UserType;

public record RegisterRequest(
        UserType userType,
        String username,
        String password,
        String displayName,
        String contactPhone
) {
}
