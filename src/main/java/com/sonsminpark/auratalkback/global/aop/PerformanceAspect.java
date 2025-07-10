package com.sonsminpark.auratalkback.global.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Slf4j
@Aspect
@Component
public class PerformanceAspect {

    @PersistenceContext
    private EntityManager entityManager;

    @Around("@annotation(org.springframework.web.bind.annotation.GetMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PutMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.DeleteMapping)")
    public Object measureApiPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();

        Statistics stats = getStatistics();
        if (stats != null) {
            stats.clear();
        }

        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();

            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;

            log.info("=== Performance Report for {} ===", methodName);
            log.info("Execution Time: {} ms", executionTime);

            if (stats != null && stats.isStatisticsEnabled()) {
                log.info("Query Count: {}", stats.getQueryExecutionCount());
                log.info("Query Execution Max Time: {} ms", stats.getQueryExecutionMaxTime());
                log.info("Entity Load Count: {}", stats.getEntityLoadCount());
                log.info("Entity Fetch Count: {}", stats.getEntityFetchCount());
                log.info("Collection Load Count: {}", stats.getCollectionLoadCount());
                log.info("Collection Fetch Count: {}", stats.getCollectionFetchCount());

                String[] queries = stats.getQueries();
                if (queries != null && queries.length > 0) {
                    log.info("Executed Queries:");
                    for (String query : queries) {
                        log.info("  - {}", query);
                    }
                }
            }
            log.info("=== End Performance Report ===");

            return result;
        } catch (Exception e) {
            log.error("Error in {}: {}", methodName, e.getMessage());
            throw e;
        }
    }

    private Statistics getStatistics() {
        try {
            Session session = entityManager.unwrap(Session.class);
            SessionFactory sessionFactory = session.getSessionFactory();
            Statistics stats = sessionFactory.getStatistics();
            stats.setStatisticsEnabled(true);
            return stats;
        } catch (Exception e) {
            log.warn("Could not get Hibernate statistics: {}", e.getMessage());
            return null;
        }
    }
}