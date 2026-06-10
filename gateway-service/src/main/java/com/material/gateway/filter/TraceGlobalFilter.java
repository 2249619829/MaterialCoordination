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

    /**
     * 作用：处理每一次经过网关或过滤器的请求。
     * 输入：
     * - exchange：当前网关请求对象，里面有请求路径、请求头和响应信息。
     * - chain：后续过滤器链，校验通过后继续调用它。
     * 输出：返回 Mono<Void>，表示一个异步执行结果；网关会等它完成后继续处理请求或写出响应。
     */
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
     * 作用：读取请求追踪 ID；如果没有，就生成一个新的。
     * 输入：
     * - exchange：当前网关请求对象，里面有请求路径、请求头和响应信息。
     * 输出：返回 String，也就是一段文本结果。
     */
    private String resolveTraceId(ServerWebExchange exchange) {
        String traceId = exchange.getRequest().getHeaders().getFirst(TraceConstants.TRACE_ID_HEADER);
        if (StringUtils.hasText(traceId)) {
            return traceId.trim();
        }
        return UUID.randomUUID().toString().replace("-", "");
    }
}
