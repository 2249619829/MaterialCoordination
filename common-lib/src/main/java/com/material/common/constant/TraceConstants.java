package com.material.common.constant;

public final class TraceConstants {
    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String TRACE_ID_MDC_KEY = "traceId";

    /**
     * 作用：创建 TraceConstants 对象，并把外部传进来的依赖保存起来。
     * 输入：
     * - 无输入参数。
     * 输出：无返回值。构造器的结果是创建好的对象本身。
     */
    private TraceConstants() {
    }
}
