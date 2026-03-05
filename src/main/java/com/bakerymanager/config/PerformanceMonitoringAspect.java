package com.bakerymanager.config;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

@Aspect
@Component
public class PerformanceMonitoringAspect {

    private static final Logger log = LoggerFactory.getLogger(PerformanceMonitoringAspect.class);

    private final Map<String, LongAdder> callCount = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> totalDurationNs = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> slowCallCount = new ConcurrentHashMap<>();

    @Value("${app.performance.monitor.enabled:true}")
    private boolean monitorEnabled;

    @Value("${app.performance.monitor.slow-threshold-ms:250}")
    private long slowThresholdMs;

    @Around("execution(* com.bakerymanager.service..*(..)) && !execution(* com.bakerymanager.service.AccessAuditService.*(..))")
    public Object monitorServiceCalls(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!monitorEnabled) {
            return joinPoint.proceed();
        }

        long start = System.nanoTime();
        try {
            return joinPoint.proceed();
        } finally {
            long elapsedNs = System.nanoTime() - start;
            long elapsedMs = elapsedNs / 1_000_000;
            String signature = joinPoint.getSignature().toShortString();

            callCount.computeIfAbsent(signature, key -> new LongAdder()).increment();
            totalDurationNs.computeIfAbsent(signature, key -> new LongAdder()).add(elapsedNs);

            if (elapsedMs >= slowThresholdMs) {
                slowCallCount.computeIfAbsent(signature, key -> new LongAdder()).increment();
                log.warn("[PERF] Slow service call: {} took {} ms", signature, elapsedMs);
            }
        }
    }

    @Scheduled(fixedDelayString = "${app.performance.monitor.report-interval-ms:300000}")
    public void reportTopServiceMetrics() {
        if (!monitorEnabled || callCount.isEmpty()) {
            return;
        }

        callCount.entrySet().stream()
            .sorted(Comparator.comparingLong(entry -> -averageMs(entry.getKey())))
            .limit(10)
            .forEach(entry -> {
                String signature = entry.getKey();
                long calls = entry.getValue().sum();
                long avgMs = averageMs(signature);
                long slowCalls = slowCallCount.getOrDefault(signature, new LongAdder()).sum();

                log.info("[PERF] {} | calls={} avg={}ms slow={} threshold={}ms",
                    signature, calls, avgMs, slowCalls, slowThresholdMs);
            });
    }

    private long averageMs(String signature) {
        long calls = callCount.getOrDefault(signature, new LongAdder()).sum();
        if (calls <= 0) {
            return 0;
        }
        long totalNs = totalDurationNs.getOrDefault(signature, new LongAdder()).sum();
        return (totalNs / calls) / 1_000_000;
    }
}
