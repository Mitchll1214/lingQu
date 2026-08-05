-- 灵渠数据接口平台 - V3：多用户与项目权限、Token 起止时间（MySQL）

-- 用户表增加状态列（0禁用 1启用）
ALTER TABLE lingqu_user
    ADD COLUMN status TINYINT NOT NULL DEFAULT 1 COMMENT '0禁用 1启用';

-- 用户-项目权限关联表（普通用户可维护的项目范围）
CREATE TABLE lingqu_project_user (
    id         VARCHAR(32)  NOT NULL,
    user_id    VARCHAR(32)  NOT NULL COMMENT '用户ID',
    project_id VARCHAR(32)  NOT NULL COMMENT '项目ID',
    created_at DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_project_user (user_id, project_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='用户-项目权限表';

-- Token 表增加生效开始时间（NULL 表示立即生效）
ALTER TABLE lingqu_token
    ADD COLUMN start_at DATETIME(3) COMMENT '生效开始时间，NULL 表示立即生效';
