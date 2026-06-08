package com.material.common.constant;

import java.time.Duration;

public final class RedisConstants {
    public static final String LOGIN_TOKEN_KEY_PREFIX = "login:token:";
    public static final Duration LOGIN_TOKEN_TTL = Duration.ofMinutes(30);

    private RedisConstants() {
    }
}
