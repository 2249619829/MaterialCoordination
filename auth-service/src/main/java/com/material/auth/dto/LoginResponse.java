package com.material.auth.dto;

import com.material.common.enums.UserType;

public record LoginResponse(String token, Long userId, UserType userType, String username, String displayName) {
}
