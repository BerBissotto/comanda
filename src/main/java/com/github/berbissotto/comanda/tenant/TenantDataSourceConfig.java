package com.github.berbissotto.comanda.tenant;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Embrulha o pool de conexões criado pelo Spring Boot (um único pool para todas as empresas)
 * com {@link TenantDataSource}. Tudo que usa o banco (JPA, JdbcTemplate) passa por ele.
 * Método estático de propósito: post-processors precisam existir antes dos demais beans.
 */
@Configuration(proxyBeanMethods = false)
public class TenantDataSourceConfig {

    @Bean
    static BeanPostProcessor tenantDataSourcePostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String nomeDoBean) {
                if (bean instanceof DataSource dataSource && !(bean instanceof TenantDataSource)) {
                    return new TenantDataSource(dataSource);
                }
                return bean;
            }
        };
    }
}
