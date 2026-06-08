package com.material.gateway.filter;

import com.material.common.constant.AuthConstants;
import com.material.common.constant.RedisConstants;
import com.material.gateway.handler.GatewayExceptionHandler;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class TokenAuthGlobalFilter implements GlobalFilter, Ordered {
    private final ReactiveStringRedisTemplate redisTemplate;
    private final GatewayExceptionHandler exceptionHandler;

    public TokenAuthGlobalFilter(ReactiveStringRedisTemplate redisTemplate,
                                 GatewayExceptionHandler exceptionHandler) {
        this.redisTemplate = redisTemplate;
        this.exceptionHandler = exceptionHandler;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (isPreflightRequest(exchange) || isPublicAuthRequest(exchange)) {
            return chain.filter(exchange);
        }

        String token = resolveToken(exchange);
        if (!StringUtils.hasText(token)) {
            return exceptionHandler.writeUnauthorized(exchange);
        }

        String redisKey = RedisConstants.LOGIN_TOKEN_KEY_PREFIX + token;
        return redisTemplate.opsForHash()
                .entries(redisKey)
                .collectMap(entry -> String.valueOf(entry.getKey()), entry -> String.valueOf(entry.getValue()))
                .flatMap(loginUser -> authenticate(exchange, chain, redisKey, loginUser));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private Mono<Void> authenticate(ServerWebExchange exchange,
                                    GatewayFilterChain chain,
                                    String redisKey,
                                    Map<String, String> loginUser) {
        if (loginUser.isEmpty() || missingRequiredUserFields(loginUser)) {
            return exceptionHandler.writeUnauthorized(exchange);
        }

        ServerWebExchange authenticatedExchange = exchange.mutate()
                .request(builder -> {
                    builder.headers(headers -> {
                        headers.set(AuthConstants.HEADER_USER_ID, loginUser.get("id"));
                        headers.set(AuthConstants.HEADER_USER_TYPE, loginUser.get("userType"));
                        headers.set(AuthConstants.HEADER_USERNAME, loginUser.get("username"));
                        headers.remove(AuthConstants.HEADER_DISPLAY_NAME);
                        String displayName = loginUser.get("displayName");
                        if (StringUtils.hasText(displayName)) {
                            headers.set(AuthConstants.HEADER_DISPLAY_NAME, displayName);
                        }
                    });
                })
                .build();

        return redisTemplate.expire(redisKey, RedisConstants.LOGIN_TOKEN_TTL)
                .onErrorResume(ex -> Mono.just(Boolean.FALSE))
                .flatMap(refreshed -> Boolean.TRUE.equals(refreshed)
                        ? chain.filter(authenticatedExchange)
                        : exceptionHandler.writeUnauthorized(exchange))
                .switchIfEmpty(exceptionHandler.writeUnauthorized(exchange));
    }

    private boolean isPublicAuthRequest(ServerWebExchange exchange) {
        String path = exchange.getRequest().getURI().getPath();
        return HttpMethod.POST.equals(exchange.getRequest().getMethod())
                && ("/auth/login".equals(path) || "/auth/register".equals(path));
    }

    private boolean isPreflightRequest(ServerWebExchange exchange) {
        return HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod());
    }

    private String resolveToken(ServerWebExchange exchange) {
        String authorization = exchange.getRequest().getHeaders().getFirst(AuthConstants.AUTHORIZATION);
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(AuthConstants.BEARER_PREFIX)) {
            return null;
        }
        return authorization.substring(AuthConstants.BEARER_PREFIX.length()).trim();
    }

    private boolean missingRequiredUserFields(Map<String, String> loginUser) {
        return !StringUtils.hasText(loginUser.get("id"))
                || !StringUtils.hasText(loginUser.get("userType"))
                || !StringUtils.hasText(loginUser.get("username"));
    }
}
