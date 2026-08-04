package com.lingqu.executor.engine;

import com.lingqu.executor.common.BizException;
import com.lingqu.executor.entity.Api;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.scripting.defaults.RawSqlSource;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * SQL 执行引擎。
 * 将数据库存储的 SQL 字符串（支持 MyBatis 动态标签 <if>/<where>/<foreach> 等）
 * 运行时解析为 MappedStatement，参数化执行，杜绝字符串拼接注入（需求 4.5.1）。
 */
@Component
public class SqlExecutor {

    private final DataSourceManager dataSourceManager;

    /** key: apiId#version#updatedAtSeconds，配置变化后自动重建 */
    private final ConcurrentMap<String, MappedStatement> statementCache = new ConcurrentHashMap<>();

    public SqlExecutor(DataSourceManager dataSourceManager) {
        this.dataSourceManager = dataSourceManager;
    }

    public Object execute(String datasourceId, Api api, Map<String, Object> params) {
        SqlSessionFactory factory = dataSourceManager.getFactory(datasourceId);
        String key = api.getId() + "#" + (api.getVersion() == null ? "" : api.getVersion())
                + "#" + (api.getUpdatedAt() == null ? 0 : api.getUpdatedAt().toEpochSecond(java.time.ZoneOffset.UTC));
        MappedStatement ms = statementCache.computeIfAbsent(key, k -> build(factory, api));
        SqlCommandType cmdType = ms.getSqlCommandType();

        try (SqlSession session = factory.openSession(false);
             Connection conn = session.getConnection()) {
            BoundSql boundSql = ms.getBoundSql(params);
            try (PreparedStatement ps = conn.prepareStatement(boundSql.getSql())) {
                ParameterHandler parameterHandler = factory.getConfiguration().newParameterHandler(ms, params, boundSql);
                parameterHandler.setParameters(ps);
                if (cmdType == SqlCommandType.SELECT) {
                    return query(ps);
                }
                int affected = ps.executeUpdate();
                // openSession(false) 关闭了自动提交，DML 必须显式提交
                session.commit();
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("affectedRows", affected);
                return result;
            }
        } catch (SQLException e) {
            throw new BizException(500, "SQL 执行失败: " + e.getMessage());
        }
    }

    private List<Map<String, Object>> query(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            ResultSetMetaData md = rs.getMetaData();
            int cols = md.getColumnCount();
            List<Map<String, Object>> rows = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= cols; i++) {
                    row.put(md.getColumnLabel(i), rs.getObject(i));
                }
                rows.add(row);
            }
            return rows;
        }
    }

    private MappedStatement build(SqlSessionFactory factory, Api api) {
        Configuration configuration = factory.getConfiguration();
        String sql = api.getSqlContent();
        // 纵深防御：无论配置来自何处，一律拒绝 ${} 字符串拼接（需求 4.5.1）
        if (sql != null && sql.contains("${")) {
            throw new BizException(500, "SQL 包含非法的 ${} 拼接，已拒绝执行（请使用 #{} 参数化）");
        }
        SqlSource sqlSource;
        if (isDynamicScript(api.getSqlContent())) {
            // 含 MyBatis 动态标签：按 XML 脚本解析
            LanguageDriver languageDriver = configuration.getLanguageDriver(null);
            sqlSource = languageDriver.createSqlSource(configuration, api.getSqlContent(), Map.class);
        } else {
            // 纯 SQL：静态解析，SQL 中的 < > 等比较符不会被误当 XML
            sqlSource = new RawSqlSource(configuration, api.getSqlContent(), Map.class);
        }
        SqlCommandType cmdType = resolveCommandType(api.getSqlContent());
        MappedStatement.Builder builder = new MappedStatement.Builder(
                configuration, "api_" + api.getId(), sqlSource, cmdType);
        builder.resultMaps(Collections.singletonList(new ResultMap.Builder(
                configuration, "apiResult_" + api.getId(), Map.class, Collections.emptyList()).build()));
        return builder.build();
    }

    /** 是否包含 MyBatis 动态标签（含 <if> 等），决定走 XML 解析还是静态解析 */
    private boolean isDynamicScript(String sql) {
        if (sql == null) {
            return false;
        }
        String lower = sql.toLowerCase();
        for (String tag : new String[]{"<if", "<where", "<foreach", "<choose", "<set", "<trim", "<bind", "<script"}) {
            if (lower.contains(tag)) {
                return true;
            }
        }
        return false;
    }

    private SqlCommandType resolveCommandType(String sql) {
        if (sql == null) {
            return SqlCommandType.SELECT;
        }
        String s = sql.trim().replaceAll("^/\\*.*?\\*/", "").trim().toUpperCase();
        if (s.startsWith("SELECT") || s.startsWith("WITH")) {
            return SqlCommandType.SELECT;
        }
        if (s.startsWith("INSERT")) {
            return SqlCommandType.INSERT;
        }
        if (s.startsWith("UPDATE")) {
            return SqlCommandType.UPDATE;
        }
        if (s.startsWith("DELETE")) {
            return SqlCommandType.DELETE;
        }
        return SqlCommandType.SELECT;
    }
}
