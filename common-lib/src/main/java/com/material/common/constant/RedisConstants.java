package com.material.common.constant;

import java.time.Duration;

public final class RedisConstants {
    public static final String LOGIN_TOKEN_KEY_PREFIX = "login:token:";
    public static final Duration LOGIN_TOKEN_TTL = Duration.ofMinutes(30);

    /**
     * 作用：创建 RedisConstants 对象，并把外部传进来的依赖保存起来。
     * 输入：
     * - 无输入参数。
     * 输出：无返回值。构造器的结果是创建好的对象本身。
     */
    private RedisConstants() {
    }
}
