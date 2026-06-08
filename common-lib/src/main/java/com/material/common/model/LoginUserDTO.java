package com.material.common.model;

import com.material.common.enums.UserType;

public record LoginUserDTO(Long id, UserType userType, String username, String displayName) {
}
