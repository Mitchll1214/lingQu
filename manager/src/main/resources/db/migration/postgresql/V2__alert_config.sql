-- 灵渠数据接口平台 - 告警规则表（PostgreSQL）
CREATE TABLE lingqu_alert_config (
    id              VARCHAR(32)   NOT NULL,
    name            VARCHAR(100)  NOT NULL,
    project_id      VARCHAR(32),
    alert_type      VARCHAR(20)   NOT NULL,
    threshold       DECIMAL(10,2) NOT NULL,
    window_minutes  INT           NOT NULL DEFAULT 5,
    silence_minutes INT           NOT NULL DEFAULT 10,
    mail_to         VARCHAR(500),
    status          SMALLINT      NOT NULL DEFAULT 1,
    last_alert_at   TIMESTAMP(3),
    created_at      TIMESTAMP(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      TIMESTAMP(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id)
);
COMMENT ON TABLE lingqu_alert_config IS '告警规则表';
