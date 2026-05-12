package com.qingtu.agent.support.trace;

import org.slf4j.MDC;

import java.util.UUID;

public class TraceIdGenerator {

    private static final String TRACE_ID_KEY = "traceId";

    public static String generate() {
        return "trace-" + UUID.randomUUID().toString().substring(0, 8);
    }

    public static String getCurrent() {
        return MDC.get(TRACE_ID_KEY);
    }

    public static void set(String traceId) {
        MDC.put(TRACE_ID_KEY, traceId);
    }

    public static void clear() {
        MDC.remove(TRACE_ID_KEY);
    }
}