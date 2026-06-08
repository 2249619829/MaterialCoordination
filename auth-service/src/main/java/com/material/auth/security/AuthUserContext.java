package com.material.auth.security;

import com.material.common.enums.ErrorCode;
import com.material.common.exception.BusinessException;
import org.springframework.util.StringUtils;

import java.util.Set;

public record AuthUserContext(Long userId, String userType, String username) {
    public static final String PURCHASER = "PURCHASER";
    public static final String SUPPLIER = "SUPPLIER";
    public static final String DRIVER = "DRIVER";
    public static final String ADMIN = "ADMIN";

    private static final Set<String> SUPPORTED_ROLES = Set.of(PURCHASER, SUPPLIER, DRIVER, ADMIN);

    public static AuthUserContext from(Long userId, String userType, String username) {
        if (userId == null) {
            throw new IllegalArgumentException("登录用户缺少用户 ID");
        }
        if (!StringUtils.hasText(userType)) {
            throw new IllegalArgumentException("登录用户缺少用户类型");
        }
        String normalizedType = userType.trim().toUpperCase();
        if (!SUPPORTED_ROLES.contains(normalizedType)) {
            throw new IllegalArgumentException("不支持的用户类型: " + userType);
        }
        return new AuthUserContext(userId, normalizedType, username);
    }

    public Long requireRole(String requiredRole) {
        if (!userType.equals(requiredRole)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return userId;
    }

}
