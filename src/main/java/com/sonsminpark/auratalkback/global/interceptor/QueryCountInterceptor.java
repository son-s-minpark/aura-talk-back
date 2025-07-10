package com.sonsminpark.auratalkback.global.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Slf4j
@Component
public class QueryCountInterceptor implements HandlerInterceptor, StatementInspector {

    private static final ThreadLocal<QueryInfo> queryInfo = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        queryInfo.set(new QueryInfo());

        log.info("========================================");
        log.info("API 요청 시작: {} {}", request.getMethod(), request.getRequestURI());
        log.info("========================================");

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        QueryInfo info = queryInfo.get();
        if (info != null) {
            long duration = System.currentTimeMillis() - info.startTime;

            log.info("========================================");
            log.info("API 요청 완료: {} {}", request.getMethod(), request.getRequestURI());
            log.info("총 실행 시간: {} ms", duration);
            log.info("실행된 쿼리 수: {} 개", info.queryCount);
            log.info("========================================\n");
        }

        queryInfo.remove();
    }

    @Override
    public String inspect(String sql) {
        QueryInfo info = queryInfo.get();
        if (info != null) {
            info.queryCount++;
            log.debug("Query #{}: {}", info.queryCount, sql);
        }
        return sql;
    }

    private static class QueryInfo {
        int queryCount = 0;
        long startTime = System.currentTimeMillis();
    }
}