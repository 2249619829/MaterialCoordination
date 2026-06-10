package com.material.common.constant;

public final class AuthConstants {
    public static final String AUTHORIZATION = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_TYPE = "X-User-Type";
    public static final String HEADER_USERNAME = "X-Username";
    public static final String HEADER_DISPLAY_NAME = "X-Display-Name";

    /**
     * 作用：创建 AuthConstants 对象，并把外部传进来的依赖保存起来。
     * 输入：
     * - 无输入参数。
     * 输出：无返回值。构造器的结果是创建好的对象本身。
     */
    private AuthConstants() {
    }
}
