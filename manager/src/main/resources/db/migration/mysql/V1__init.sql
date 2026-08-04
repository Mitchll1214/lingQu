-- =====================================================================
-- 灵渠数据接口平台 - 配置库初始化（MySQL 5.7+）
-- 说明：需求文档中的 JSON 类型字段统一用 TEXT 存储 JSON 字符串，
--       由应用层保证 JSON 合法性，保证 MySQL/PostgreSQL 完全兼容。
-- =====================================================================

CREATE TABLE lingqu_user (
    id            VARCHAR(32)  NOT NULL,
    username      VARCHAR(50)  NOT NULL,
    password_hash VARCHAR(200) NOT NULL,
    role          VARCHAR(20)  NOT NULL DEFAULT 'ADMIN',
    created_at    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='用户表';

CREATE TABLE lingqu_datasource (
    id           VARCHAR(32)  NOT NULL,
    name         VARCHAR(100) NOT NULL,
    db_type      VARCHAR(50)  NOT NULL,
    driver_class VARCHAR(200),
    jdbc_url     VARCHAR(500) NOT NULL,
    username     VARCHAR(100),
    password     VARCHAR(200) NOT NULL COMMENT 'AES 加密存储',
    pool_config  TEXT COMMENT '连接池配置(JSON字符串)',
    status       TINYINT      NOT NULL DEFAULT 1 COMMENT '0不可用 1可用',
    created_at   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='数据源表';

CREATE TABLE lingqu_project (
    id            VARCHAR(32)  NOT NULL,
    name          VARCHAR(100) NOT NULL,
    code          VARCHAR(50)  NOT NULL,
    route_prefix  VARCHAR(100) NOT NULL,
    description   VARCHAR(500),
    department    VARCHAR(100) COMMENT '所属部门',
    datasource_id VARCHAR(32),
    auth_type     VARCHAR(20)  NOT NULL DEFAULT 'none' COMMENT 'none/token/apikey',
    auth_config   TEXT COMMENT '认证配置(JSON字符串)',
    status        TINYINT      NOT NULL DEFAULT 1 COMMENT '0禁用 1启用',
    deleted       TINYINT      NOT NULL DEFAULT 0 COMMENT '0正常 1已删除(软删)',
    created_at    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_project_code (code),
    UNIQUE KEY uk_project_route (route_prefix)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='项目表';

CREATE TABLE lingqu_api (
    id              VARCHAR(32)  NOT NULL,
    project_id      VARCHAR(32)  NOT NULL,
    api_name        VARCHAR(100) NOT NULL,
    api_path        VARCHAR(200) NOT NULL,
    method          VARCHAR(10)  NOT NULL,
    sql_content     TEXT COMMENT 'SQL 或 Groovy 脚本',
    sql_type        VARCHAR(20)  NOT NULL DEFAULT 'sql' COMMENT 'sql/groovy',
    params          TEXT COMMENT '入参定义(JSON字符串)',
    response_format TEXT COMMENT '出参映射(JSON字符串)',
    log_enabled     TINYINT      NOT NULL DEFAULT 0 COMMENT '日志开关 0关 1开',
    rate_limit_qps  DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT 'QPS限制(0不限)',
    status          TINYINT      NOT NULL DEFAULT 0 COMMENT '0草稿 1上线 2下线',
    version         VARCHAR(20),
    created_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_api_project_path (project_id, api_path)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='接口表';

CREATE TABLE lingqu_token (
    id         VARCHAR(32)  NOT NULL,
    project_id VARCHAR(32)  NOT NULL,
    token      VARCHAR(256) NOT NULL COMMENT 'AES 加密存储',
    token_name VARCHAR(100) COMMENT '标识名称',
    expire_at  DATETIME(3),
    status     TINYINT      NOT NULL DEFAULT 1 COMMENT '0无效 1有效',
    created_at DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_token_project (project_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='Token表';

CREATE TABLE lingqu_api_log (
    id             VARCHAR(32)   NOT NULL,
    project_id     VARCHAR(32),
    project_code   VARCHAR(50),
    api_id         VARCHAR(32),
    api_name       VARCHAR(100),
    request_path   VARCHAR(500),
    request_method VARCHAR(10),
    request_params TEXT COMMENT '请求参数(JSON字符串)',
    response_data  TEXT COMMENT '响应数据',
    status_code    INT,
    cost_time      BIGINT COMMENT '耗时(毫秒)',
    client_ip      VARCHAR(50),
    error_msg      VARCHAR(1000),
    created_at     DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_log_project_time (project_id, created_at),
    KEY idx_log_api_time (api_id, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='调用日志表';
