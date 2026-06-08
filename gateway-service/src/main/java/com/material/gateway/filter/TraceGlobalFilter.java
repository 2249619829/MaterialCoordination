package com.material.gateway.filter;

import com.material.common.constant.TraceConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class TraceGlobalFilter implements GlobalFilter, Ordered {
    private static final Logger log = LoggerFactory.getLogger(TraceGlobalFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = resolveTraceId(exchange);
        long startNanos = System.nanoTime();
        ServerWebExchange tracedExchange = exchange.mutate()
                .request(builder -> builder.headers(headers -> headers.set(TraceConstants.TRACE_ID_HEADER, traceId)))
                .build();
        tracedExchange.getResponse().getHeaders().set(TraceConstants.TRACE_ID_HEADER, traceId);
        return chain.filter(tracedExchange)
                .doFinally(signalType -> log.info("gateway_request traceId={} method={} path={} status={} costMs={}",
                        traceId,
                        exchange.getRequest().getMethod(),
                        exchange.getRequest().getURI().getPath(),
                        tracedExchange.getResponse().getStatusCode(),
                        (System.nanoTime() - startNanos) / 1_000_000));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private String resolveTraceId(ServerWebExchange exchange) {
        String traceId = exchange.getRequest().getHeaders().getFirst(TraceConstants.TRACE_ID_HEADER);
        if (StringUtils.hasText(traceId)) {
            return traceId.trim();
        }
        return UUID.randomUUID().toString().replace("-", "");
    }
}
