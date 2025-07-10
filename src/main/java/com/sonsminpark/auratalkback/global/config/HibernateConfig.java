package com.sonsminpark.auratalkback.global.config;

import com.sonsminpark.auratalkback.global.interceptor.QueryCountInterceptor;
import org.hibernate.cfg.AvailableSettings;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HibernateConfig {

    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer(QueryCountInterceptor queryCountInterceptor) {
        return hibernateProperties -> {
            hibernateProperties.put(AvailableSettings.STATEMENT_INSPECTOR, queryCountInterceptor);
        };
    }
}