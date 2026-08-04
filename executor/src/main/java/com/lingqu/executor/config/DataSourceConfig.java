package com.lingqu.executor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;

/**
 * 配置库数据源（与 Manager 相同逻辑）。
 */
@Configuration
public class DataSourceConfig {

    @Value("${DB_TYPE:mysql}")
    private String dbType;

    @Value("${DB_HOST:localhost}")
    private String host;

    @Value("${DB_PORT:}")
    private String port;

    @Value("${DB_NAME:lingqu}")
    private String dbName;

    @Value("${DB_USER:root}")
    private String user;

    @Value("${DB_PASSWORD:}")
    private String password;

    @Bean
    @Primary
    public DataSource dataSource() {
        String type = normalizeType(dbType);
        String p = (port == null || port.isEmpty()) ? (type.equals("postgresql") ? "5432" : "3306") : port;

        HikariDataSource ds = new HikariDataSource();
        if (type.equals("postgresql")) {
            ds.setDriverClassName("org.postgresql.Driver");
            ds.setJdbcUrl("jdbc:postgresql://" + host + ":" + p + "/" + dbName);
        } else {
            ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
            ds.setJdbcUrl("jdbc:mysql://" + host + ":" + p + "/" + dbName
                    + "?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai"
                    + "&useSSL=false&allowPublicKeyRetrieval=true");
        }
        ds.setUsername(user);
        ds.setPassword(password);
        ds.setMaximumPoolSize(10);
        ds.setMinimumIdle(1);
        return ds;
    }

    public static String normalizeType(String type) {
        if (type == null) {
            return "mysql";
        }
        String t = type.trim().toLowerCase();
        if (t.equals("postgresql") || t.equals("pg") || t.equals("postgres")) {
            return "postgresql";
        }
        return "mysql";
    }
}
