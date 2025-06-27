package com.receiptanalyzer.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/receipt_analyzer";
    private static final String USER = "postgres";
    private static final String PASS = "123456";
    private static final String DRIVER_CLASS_NAME = "org.postgresql.Driver";

    @Bean
    public static DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(DB_URL);
        config.setUsername(USER);
        config.setPassword(PASS);
        config.setDriverClassName(DRIVER_CLASS_NAME);
        config.setMaximumPoolSize(10); // Максимальное число соединений в пуле
        config.setConnectionTimeout(30000); // 30 секунд
        return new HikariDataSource(config);
    }

    @Bean
    public JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(dataSource());
    }
}
