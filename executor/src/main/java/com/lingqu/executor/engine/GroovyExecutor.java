package com.lingqu.executor.engine;

import com.lingqu.executor.common.BizException;
import com.lingqu.executor.entity.Api;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

/**
 * Groovy 脚本执行器（SQL 的替代或补充，需求 3.3.6）。
 * 脚本中可用变量：
 *   - params：请求参数 Map
 *   - sql：SqlHelper 对象，提供 query(sql, args...) / update(sql, args...) 方法
 * 脚本返回值将作为接口响应。
 */
@Component
public class GroovyExecutor {

    private final DataSourceManager dataSourceManager;

    public GroovyExecutor(DataSourceManager dataSourceManager) {
        this.dataSourceManager = dataSourceManager;
    }

    public Object execute(String datasourceId, Api api, Map<String, Object> params) {
        DataSource ds = dataSourceManager.getRawDataSource(datasourceId);
        Binding binding = new Binding();
        binding.setVariable("params", params);
        binding.setVariable("sql", new SqlHelper(new JdbcTemplate(ds)));
        GroovyShell shell = new GroovyShell(binding);
        try {
            return shell.evaluate(api.getSqlContent());
        } catch (Exception e) {
            throw new BizException(500, "Groovy 脚本执行失败: " + e.getMessage());
        }
    }

    /**
     * 提供给脚本的 SQL 辅助对象，方法参数均为参数化绑定，无注入风险。
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static class SqlHelper {

        private final JdbcTemplate jdbcTemplate;

        public SqlHelper(JdbcTemplate jdbcTemplate) {
            this.jdbcTemplate = jdbcTemplate;
        }

        public List<Map<String, Object>> query(String sql, Object... args) {
            return (List) jdbcTemplate.queryForList(sql, args);
        }

        public int update(String sql, Object... args) {
            return jdbcTemplate.update(sql, args);
        }
    }
}
