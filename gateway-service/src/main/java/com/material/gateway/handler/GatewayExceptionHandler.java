package com.material.gateway.handler;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
public class GatewayExceptionHandler {
    private static final byte[] UNAUTHORIZED_BODY =
            "{\"code\":401,\"message\":\"unauthorized\",\"data\":null}".getBytes(StandardCharsets.UTF_8);

    /**
     * 作用：向浏览器或前端返回未登录的错误结果。
     * 输入：
     * - exchange：当前网关请求对象，里面有请求路径、请求头和响应信息。
     * 输出：返回 Mono<Void>，表示一个异步执行结果；网关会等它完成后继续处理请求或写出响应。
     */
    public Mono<Void> writeUnauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(UNAUTHORIZED_BODY);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
