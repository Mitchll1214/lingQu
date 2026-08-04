package com.lingqu.manager.common;

import com.lingqu.manager.config.DataSourceConfig;

/**
 * 数据库方言工具，根据环境变量 DB_TYPE 判断配置库类型。
 */
public final class Dialect {

    private Dialect() {
    }

    public static String type() {
        return DataSourceConfig.normalizeType(System.getenv("DB_TYPE"));
    }

    public static boolean isPostgresql() {
        return "postgresql".equals(type());
    }

    /**
     * 供 MyBatis-Plus 分页插件使用的方言枚举名。
     */
    public static String mybatisPlusDbType() {
        return isPostgresql() ? "POSTGRE_SQL" : "MYSQL";
    }
}
