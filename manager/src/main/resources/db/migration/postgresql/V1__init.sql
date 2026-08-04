-- =====================================================================
-- 灵渠数据接口平台 - 配置库初始化（PostgreSQL 12+）
-- 说明：需求文档中的 JSON 类型字段统一用 TEXT 存储 JSON 字符串，
--       由应用层保证 JSON 合法性，保证 MySQL/PostgreSQL 完全兼容。
-- =====================================================================

CREATE TABLE lingqu_user (
    id            VARCHAR(32)  NOT NULL,
    username      VARCHAR(50)  NOT NULL,
    password_hash VARCHAR(200) NOT NULL,
    role          VARCHAR(20)  NOT NULL DEFAULT 'ADMIN',
    created_at    TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    CONSTRAINT uk_user_username UNIQUE (username)
);
COMMENT ON TABLE lingqu_user IS '用户表';

CREATE TABLE lingqu_datasource (
    id           VARCHAR(32)  NOT NULL,
    name         VARCHAR(100) NOT NULL,
    db_type      VARCHAR(50)  NOT NULL,
    driver_class VARCHAR(200),
    jdbc_url     VARCHAR(500) NOT NULL,
    username     VARCHAR(100),
    password     VARCHAR(200) NOT NULL,
    pool_config  TEXT,
    status       SMALLINT     NOT NULL DEFAULT 1,
    created_at   TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at   TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id)
);
COMMENT ON TABLE lingqu_datasource IS '数据源表';

CREATE TABLE lingqu_project (
    id            VARCHAR(32)  NOT NULL,
    name          VARCHAR(100) NOT NULL,
    code          VARCHAR(50)  NOT NULL,
    route_prefix  VARCHAR(100) NOT NULL,
    description   VARCHAR(500),
    department    VARCHAR(100),
    datasource_id VARCHAR(32),
    auth_type     VARCHAR(20)  NOT NULL DEFAULT 'none',
    auth_config   TEXT,
    status        SMALLINT     NOT NULL DEFAULT 1,
    deleted       SMALLINT     NOT NULL DEFAULT 0,
    created_at    TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at    TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    CONSTRAINT uk_project_code UNIQUE (code),
    CONSTRAINT uk_project_route UNIQUE (route_prefix)
);
COMMENT ON TABLE lingqu_project IS '项目表';

CREATE TABLE lingqu_api (
    id              VARCHAR(32)   NOT NULL,
    project_id      VARCHAR(32)   NOT NULL,
    api_name        VARCHAR(100)  NOT NULL,
    api_path        VARCHAR(200)  NOT NULL,
    method          VARCHAR(10)   NOT NULL,
    sql_content     TEXT,
    sql_type        VARCHAR(20)   NOT NULL DEFAULT 'sql',
    params          TEXT,
    response_format TEXT,
    log_enabled     SMALLINT      NOT NULL DEFAULT 0,
    rate_limit_qps  DECIMAL(10,2) NOT NULL DEFAULT 0,
    status          SMALLINT      NOT NULL DEFAULT 0,
    version         VARCHAR(20),
    created_at      TIMESTAMP(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      TIMESTAMP(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    CONSTRAINT uk_api_project_path UNIQUE (project_id, api_path)
);
COMMENT ON TABLE lingqu_api IS '接口表';

CREATE TABLE lingqu_token (
    id         VARCHAR(32)  NOT NULL,
    project_id VARCHAR(32)  NOT NULL,
    token      VARCHAR(256) NOT NULL,
    token_name VARCHAR(100),
    expire_at  TIMESTAMP(3),
    status     SMALLINT     NOT NULL DEFAULT 1,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id)
);
CREATE INDEX idx_token_project ON lingqu_token (project_id);
COMMENT ON TABLE lingqu_token IS 'Token表';

CREATE TABLE lingqu_api_log (
    id             VARCHAR(32)   NOT NULL,
    project_id     VARCHAR(32),
    project_code   VARCHAR(50),
    api_id         VARCHAR(32),
    api_name       VARCHAR(100),
    request_path   VARCHAR(500),
    request_method VARCHAR(10),
    request_params TEXT,
    response_data  TEXT,
    status_code    INT,
    cost_time      BIGINT,
    client_ip      VARCHAR(50),
    error_msg      VARCHAR(1000),
    created_at     TIMESTAMP(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id)
);
CREATE INDEX idx_log_project_time ON lingqu_api_log (project_id, created_at);
CREATE INDEX idx_log_api_time ON lingqu_api_log (api_id, created_at);
COMMENT ON TABLE lingqu_api_log IS '调用日志表';
