package com.material.gateway.config;

import com.material.common.constant.AuthConstants;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.test.StepVerifier;

class RateLimitKeyConfigTest {
    private final KeyResolver keyResolver = new RateLimitKeyConfig().rateLimitKeyResolver();

    /**
     * 作用：测试已经通过 Token 校验的请求，会优先用用户编号作为限流 key。
     * 输入：
     * - 无输入参数；测试内部会创建一个带 X-User-Id 请求头的模拟请求。
     * 输出：无返回值。断言通过表示用户级限流 key 生成正确。
     */
    @Test
    void usesUserIdWhenTrustedUserHeaderExists() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/suppliers/catalog")
                        .header(AuthConstants.HEADER_USER_ID, "42")
                        .build()
        );

        StepVerifier.create(keyResolver.resolve(exchange))
                .expectNext("user:42")
                .verifyComplete();
    }

    /**
     * 作用：测试还没写入用户编号时，会用 Bearer Token 的摘要作为限流 key。
     * 输入：
     * - 无输入参数；测试内部会创建一个带 Authorization 请求头的模拟请求。
     * 输出：无返回值。断言通过表示 Token 级限流 key 生成正确，而且不会直接暴露原始 Token。
     */
    @Test
    void usesTokenHashWhenBearerTokenExists() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/suppliers/catalog")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token-123")
                        .build()
        );

        StepVerifier.create(keyResolver.resolve(exchange))
                .expectNextMatches(key -> key.startsWith("token:")
                        && !key.contains("token-123")
                        && key.length() > "token:".length())
                .verifyComplete();
    }

    /**
     * 作用：测试登录、注册这类没有 Token 的请求，会用客户端 IP 作为限流 key。
     * 输入：
     * - 无输入参数；测试内部会创建一个带 X-Forwarded-For 请求头的模拟请求。
     * 输出：无返回值。断言通过表示 IP 级限流 key 生成正确。
     */
    @Test
    void usesFirstForwardedIpWhenNoUserOrTokenExists() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/auth/login")
                        .header("X-Forwarded-For", "10.0.0.8, 10.0.0.9")
                        .build()
        );

        StepVerifier.create(keyResolver.resolve(exchange))
                .expectNext("ip:10.0.0.8")
                .verifyComplete();
    }
}
