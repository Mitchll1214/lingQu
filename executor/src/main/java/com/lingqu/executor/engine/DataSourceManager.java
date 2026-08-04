package com.lingqu.executor.engine;

import com.lingqu.executor.cache.ConfigCache;
import com.lingqu.executor.common.BizException;
import com.lingqu.executor.common.CryptoUtil;
import com.lingqu.executor.entity.Datasource;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 业务数据源连接池管理器。
 * 每个数据源对应一个独立 SqlSessionFactory（HikariCP 连接池）；
 * 数据源配置更新后（updatedAt 变化）自动重建连接池（30s 刷新周期内生效）。
 */
@Component
public class DataSourceManager {

    private final ConfigCache configCache;

    private final ConcurrentMap<String, SqlSessionFactory> byId = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> signatures = new ConcurrentHashMap<>();

    public DataSourceManager(ConfigCache configCache) {
        this.configCache = configCache;
    }

    public SqlSessionFactory getFactory(String datasourceId) {
        Datasource ds = configCache.getDatasource(datasourceId);
        if (ds == null) {
            throw new BizException(500, "数据源配置不存在: " + datasourceId);
        }
        if (ds.getStatus() != null && ds.getStatus() == 0) {
            throw new BizException(500, "数据源当前不可用: " + ds.getName());
        }
        String sig = ds.getId() + "#" + (ds.getUpdatedAt() == null ? "" : ds.getUpdatedAt().toString());
        SqlSessionFactory existing = byId.get(datasourceId);
        if (existing != null && sig.equals(signatures.get(datasourceId))) {
            return existing;
        }
        synchronized (this) {
            existing = byId.get(datasourceId);
            if (existing != null && sig.equals(signatures.get(datasourceId))) {
                return existing;
            }
            SqlSessionFactory factory = build(ds);
            SqlSessionFactory old = byId.put(datasourceId, factory);
            signatures.put(datasourceId, sig);
            if (old != null) {
                closeQuietly(old);
            }
            return factory;
        }
    }

    public DataSource getRawDataSource(String datasourceId) {
        return (DataSource) getFactory(datasourceId).getConfiguration().getEnvironment().getDataSource();
    }

    private SqlSessionFactory build(Datasource ds) {
        HikariDataSource hds = new HikariDataSource();
        hds.setDriverClassName(ds.getDriverClass() == null || ds.getDriverClass().isEmpty()
                ? guessDriver(ds) : ds.getDriverClass());
        hds.setJdbcUrl(ds.getJdbcUrl());
        hds.setUsername(ds.getUsername());
        hds.setPassword(CryptoUtil.decrypt(ds.getPassword()));
        hds.setMaximumPoolSize(10);
        hds.setMinimumIdle(0);
        hds.setConnectionTimeout(5000);

        Configuration configuration = new Configuration();
        configuration.setMapUnderscoreToCamelCase(false);
        configuration.setEnvironment(new Environment("ds-" + ds.getId(), new JdbcTransactionFactory(), hds));
        return new SqlSessionFactoryBuilder().build(configuration);
    }

    private String guessDriver(Datasource ds) {
        String type = ds.getDbType() == null ? "" : ds.getDbType().toLowerCase();
        if (type.contains("postgres")) {
            return "org.postgresql.Driver";
        }
        return "com.mysql.cj.jdbc.Driver";
    }

    private void closeQuietly(SqlSessionFactory factory) {
        try {
            Object ds = factory.getConfiguration().getEnvironment().getDataSource();
            if (ds instanceof HikariDataSource) {
                ((HikariDataSource) ds).close();
            }
        } catch (Exception ignored) {
            // 旧连接池关闭失败不影响新连接池
        }
    }
}
