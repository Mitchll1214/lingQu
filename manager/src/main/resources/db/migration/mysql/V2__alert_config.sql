-- 灵渠数据接口平台 - 告警规则表（MySQL）
CREATE TABLE lingqu_alert_config (
    id              VARCHAR(32)   NOT NULL,
    name            VARCHAR(100)  NOT NULL COMMENT '规则名称',
    project_id      VARCHAR(32) COMMENT '目标项目ID，NULL表示全局',
    alert_type      VARCHAR(20)   NOT NULL COMMENT 'timeout=响应超时 / error_rate=错误率',
    threshold       DECIMAL(10,2) NOT NULL COMMENT 'timeout:秒; error_rate:百分比',
    window_minutes  INT           NOT NULL DEFAULT 5 COMMENT '统计窗口(分钟)',
    silence_minutes INT           NOT NULL DEFAULT 10 COMMENT '防抖静默(分钟)',
    mail_to         VARCHAR(500) COMMENT '收件人(逗号分隔)，空则用全局默认',
    status          TINYINT       NOT NULL DEFAULT 1 COMMENT '0禁用 1启用',
    last_alert_at   DATETIME(3),
    created_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='告警规则表';
