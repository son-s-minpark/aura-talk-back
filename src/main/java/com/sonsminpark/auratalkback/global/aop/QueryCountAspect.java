package com.sonsminpark.auratalkback.global.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class QueryCountAspect {

    private static final ThreadLocal<QueryCounter> queryCounter = new ThreadLocal<>();

    @Around("execution(* com.sonsminpark.auratalkback.domain.*.service.*.*(..))")
    public Object countQueries(ProceedingJoinPoint joinPoint) throws Throwable {
        boolean isFirstCall = false;

        if (queryCounter.get() == null) {
            queryCounter.set(new QueryCounter());
            isFirstCall = true;
        }

        QueryCounter counter = queryCounter.get();
        String methodName = joinPoint.getSignature().toShortString();

        try {
            counter.startMethod(methodName);
            Object result = joinPoint.proceed();
            counter.endMethod(methodName);

            if (isFirstCall) {
                log.info("Query Statistics for {}: {}", methodName, counter.getStatistics());
                queryCounter.remove();
            }

            return result;
        } catch (Exception e) {
            if (isFirstCall) {
                queryCounter.remove();
            }
            throw e;
        }
    }

    public static class QueryCounter {
        private int queryCount = 0;
        private long startTime;
        private String currentMethod;

        public void startMethod(String method) {
            this.currentMethod = method;
            this.startTime = System.currentTimeMillis();
        }

        public void endMethod(String method) {
        }

        public void incrementQueryCount() {
            this.queryCount++;
        }

        public String getStatistics() {
            long duration = System.currentTimeMillis() - startTime;
            return String.format("Queries: %d, Duration: %d ms", queryCount, duration);
        }
    }
}
