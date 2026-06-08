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

    public Mono<Void> writeUnauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(UNAUTHORIZED_BODY);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
