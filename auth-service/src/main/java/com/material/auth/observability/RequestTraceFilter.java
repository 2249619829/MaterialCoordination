package com.material.auth.observability;

import com.material.common.constant.AuthConstants;
import com.material.common.constant.TraceConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class RequestTraceFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RequestTraceFilter.class);

    /**
     * 作用：完成 doFilterInternal 这一步处理。
     * 输入：
     * - request：前端传来的请求数据对象，里面包含本次操作需要的信息。
     * - response：接口返回对象，方法会把结果写到这里。
     * - filterChain：Filter Chain，类型是 FilterChain；方法会读取这个值继续处理。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = resolveTraceId(request);
        long startNanos = System.nanoTime();
        MDC.put(TraceConstants.TRACE_ID_MDC_KEY, traceId);
        response.setHeader(TraceConstants.TRACE_ID_HEADER, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            long costMs = (System.nanoTime() - startNanos) / 1_000_000;
            log.info("http_request traceId={} method={} uri={} status={} costMs={} userId={} userType={}",
                    traceId,
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    costMs,
                    request.getHeader(AuthConstants.HEADER_USER_ID),
                    request.getHeader(AuthConstants.HEADER_USER_TYPE));
            MDC.remove(TraceConstants.TRACE_ID_MDC_KEY);
        }
    }

    /**
     * 作用：读取请求追踪 ID；如果没有，就生成一个新的。
     * 输入：
     * - request：前端传来的请求数据对象，里面包含本次操作需要的信息。
     * 输出：返回 String，也就是一段文本结果。
     */
    private String resolveTraceId(HttpServletRequest request) {
        String traceId = request.getHeader(TraceConstants.TRACE_ID_HEADER);
        if (StringUtils.hasText(traceId)) {
            return traceId.trim();
        }
        return UUID.randomUUID().toString().replace("-", "");
    }
}
