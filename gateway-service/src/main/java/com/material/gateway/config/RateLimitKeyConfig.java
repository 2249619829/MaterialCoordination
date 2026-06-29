package com.material.gateway.config;

import com.material.common.constant.AuthConstants;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Configuration
public class RateLimitKeyConfig {
    private static final int HASH_PREFIX_LENGTH = 32;

    /**
     * 作用：创建网关限流用的 key 解析器，让 Spring Cloud Gateway 知道应该按谁来扣令牌。
     * 输入：
     * - 无输入参数；Spring 会在启动时自动调用这个方法。
     * 输出：返回 KeyResolver，网关限流过滤器会用它给每个请求生成一个限流 key。
     */
    @Bean
    public KeyResolver rateLimitKeyResolver() {
        return exchange -> Mono.just(resolveRateLimitKey(exchange));
    }

    /**
     * 作用：为一次请求生成限流 key，优先级是用户编号、Token 摘要、客户端 IP。
     * 输入：
     * - exchange：当前网关请求对象，里面有请求头、路径和客户端地址。
     * 输出：返回 String，表示本次请求应该归到哪个限流桶里。
     */
    private String resolveRateLimitKey(ServerWebExchange exchange) {
        String userId = exchange.getRequest().getHeaders().getFirst(AuthConstants.HEADER_USER_ID);
        if (StringUtils.hasText(userId)) {
            return "user:" + safeKeyPart(userId);
        }

        String token = resolveBearerToken(exchange);
        if (StringUtils.hasText(token)) {
            return "token:" + sha256Prefix(token);
        }

        return "ip:" + safeKeyPart(resolveClientIp(exchange));
    }

    /**
     * 作用：从 Authorization 请求头里取出 Bearer Token。
     * 输入：
     * - exchange：当前网关请求对象，里面有请求头信息。
     * 输出：返回 String；如果请求头里没有合法 Token，就返回 null。
     */
    private String resolveBearerToken(ServerWebExchange exchange) {
        String authorization = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(AuthConstants.BEARER_PREFIX)) {
            return null;
        }
        return authorization.substring(AuthConstants.BEARER_PREFIX.length()).trim();
    }

    /**
     * 作用：解析客户端 IP，优先使用代理传来的 X-Forwarded-For。
     * 输入：
     * - exchange：当前网关请求对象，里面可能包含代理请求头和远程地址。
     * 输出：返回 String，表示客户端 IP；如果确实拿不到，就返回 unknown。
     */
    private String resolveClientIp(ServerWebExchange exchange) {
        String forwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }

        String realIp = exchange.getRequest().getHeaders().getFirst("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp.trim();
        }

        if (exchange.getRequest().getRemoteAddress() != null
                && exchange.getRequest().getRemoteAddress().getAddress() != null) {
            return exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        }

        return "unknown";
    }

    /**
     * 作用：把 Token 变成短摘要，避免把原始 Token 放进 Redis 限流 key。
     * 输入：
     * - value：原始 Token 文本。
     * 输出：返回 String，表示 Token 的 SHA-256 摘要前缀。
     */
    private String sha256Prefix(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
            return encoded.substring(0, Math.min(HASH_PREFIX_LENGTH, encoded.length()));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", ex);
        }
    }

    /**
     * 作用：清理限流 key 中的特殊字符，避免不同来源的值把 Redis key 搞乱。
     * 输入：
     * - value：原始 key 片段，比如用户编号或 IP。
     * 输出：返回 String，表示可以安全放进限流 key 的文本。
     */
    private String safeKeyPart(String value) {
        return value.trim().replaceAll("[^a-zA-Z0-9._:-]", "_");
    }
}
