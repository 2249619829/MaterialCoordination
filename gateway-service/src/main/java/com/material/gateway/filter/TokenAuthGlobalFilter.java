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

    /**
     * 作用：创建 TokenAuthGlobalFilter 对象，并把外部传进来的依赖保存起来。
     * 输入：
     * - redisTemplate：Redis 操作工具，类型是 ReactiveStringRedisTemplate；方法会读取这个值继续处理。
     * - exceptionHandler：网关异常处理器，类型是 GatewayExceptionHandler；方法会读取这个值继续处理。
     * 输出：无返回值。构造器的结果是创建好的对象本身。
     */
    public TokenAuthGlobalFilter(ReactiveStringRedisTemplate redisTemplate,
                                 GatewayExceptionHandler exceptionHandler) {
        this.redisTemplate = redisTemplate;
        this.exceptionHandler = exceptionHandler;
    }

    /**
     * 作用：处理每一次经过网关或过滤器的请求。
     * 输入：
     * - exchange：当前网关请求对象，里面有请求路径、请求头和响应信息。
     * - chain：后续过滤器链，校验通过后继续调用它。
     * 输出：返回 Mono<Void>，表示一个异步执行结果；网关会等它完成后继续处理请求或写出响应。
     */
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

    /**
     * 作用：告诉 Spring 这个过滤器应该在什么顺序执行。
     * 输入：
     * - 无输入参数。
     * 输出：返回 int，表示过滤器的执行顺序；数字越小通常越早执行。
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    /**
     * 作用：校验 Redis 登录态，并把可信用户信息传给下游服务。
     * 输入：
     * - exchange：当前网关请求对象，里面有请求路径、请求头和响应信息。
     * - chain：后续过滤器链，校验通过后继续调用它。
     * - redisKey：Redis 中保存数据时使用的键名。
     * - loginUser：从 Redis 读出的登录用户信息，包含用户编号、类型和用户名。
     * 输出：返回 Mono<Void>，表示一个异步执行结果；网关会等它完成后继续处理请求或写出响应。
     */
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

    /**
     * 作用：判断当前请求是否是登录或注册请求。
     * 输入：
     * - exchange：当前网关请求对象，里面有请求路径、请求头和响应信息。
     * 输出：返回 boolean，true 表示条件成立，false 表示条件不成立。
     */
    private boolean isPublicAuthRequest(ServerWebExchange exchange) {
        String path = exchange.getRequest().getURI().getPath();
        return HttpMethod.POST.equals(exchange.getRequest().getMethod())
                && ("/auth/login".equals(path) || "/auth/register".equals(path));
    }

    /**
     * 作用：判断当前请求是否是浏览器的 CORS 预检请求。
     * 输入：
     * - exchange：当前网关请求对象，里面有请求路径、请求头和响应信息。
     * 输出：返回 boolean，true 表示条件成立，false 表示条件不成立。
     */
    private boolean isPreflightRequest(ServerWebExchange exchange) {
        return HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod());
    }

    /**
     * 作用：从请求头里解析 Bearer Token。
     * 输入：
     * - exchange：当前网关请求对象，里面有请求路径、请求头和响应信息。
     * 输出：返回 String，也就是一段文本结果。
     */
    private String resolveToken(ServerWebExchange exchange) {
        String authorization = exchange.getRequest().getHeaders().getFirst(AuthConstants.AUTHORIZATION);
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(AuthConstants.BEARER_PREFIX)) {
            return null;
        }
        return authorization.substring(AuthConstants.BEARER_PREFIX.length()).trim();
    }

    /**
     * 作用：判断 Redis 登录态是否缺少用户编号、类型或用户名。
     * 输入：
     * - loginUser：从 Redis 读出的登录用户信息，包含用户编号、类型和用户名。
     * 输出：返回 boolean，true 表示条件成立，false 表示条件不成立。
     */
    private boolean missingRequiredUserFields(Map<String, String> loginUser) {
        return !StringUtils.hasText(loginUser.get("id"))
                || !StringUtils.hasText(loginUser.get("userType"))
                || !StringUtils.hasText(loginUser.get("username"));
    }
}
