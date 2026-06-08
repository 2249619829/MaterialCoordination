package com.material.auth.dto;

import com.material.common.enums.UserType;

public record LoginRequest(UserType userType, String username, String password) {
}
