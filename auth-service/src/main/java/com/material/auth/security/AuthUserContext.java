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

    /**
     * 作用：根据请求头里的用户信息创建当前用户对象。
     * 输入：
     * - userId：用户编号，类型是 Long；方法会读取这个值继续处理。
     * - userType：用户类型，类型是 String；方法会读取这个值继续处理。
     * - username：用户名，类型是 String；方法会读取这个值继续处理。
     * 输出：返回 AuthUserContext，也就是这个方法处理后的结果。
     */
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

    /**
     * 作用：检查当前用户是否是指定角色。
     * 输入：
     * - requiredRole：要求的角色，类型是 String；方法会读取这个值继续处理。
     * 输出：返回 Long，表示方法算出的数量、编号或顺序值。
     */
    public Long requireRole(String requiredRole) {
        if (!userType.equals(requiredRole)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return userId;
    }

}
