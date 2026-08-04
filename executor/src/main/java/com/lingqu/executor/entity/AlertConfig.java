package com.lingqu.executor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 告警规则表（Executor 只读）。
 */
@TableName("lingqu_alert_config")
public class AlertConfig {

    public static final String TYPE_TIMEOUT = "timeout";
    public static final String TYPE_ERROR_RATE = "error_rate";

    @TableId(type = IdType.INPUT)
    private String id;

    private String name;

    private String projectId;

    private String alertType;

    private BigDecimal threshold;

    private Integer windowMinutes;

    private Integer silenceMinutes;

    private String mailTo;

    private Integer status;

    private LocalDateTime lastAlertAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getAlertType() {
        return alertType;
    }

    public void setAlertType(String alertType) {
        this.alertType = alertType;
    }

    public BigDecimal getThreshold() {
        return threshold;
    }

    public void setThreshold(BigDecimal threshold) {
        this.threshold = threshold;
    }

    public Integer getWindowMinutes() {
        return windowMinutes;
    }

    public void setWindowMinutes(Integer windowMinutes) {
        this.windowMinutes = windowMinutes;
    }

    public Integer getSilenceMinutes() {
        return silenceMinutes;
    }

    public void setSilenceMinutes(Integer silenceMinutes) {
        this.silenceMinutes = silenceMinutes;
    }

    public String getMailTo() {
        return mailTo;
    }

    public void setMailTo(String mailTo) {
        this.mailTo = mailTo;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getLastAlertAt() {
        return lastAlertAt;
    }

    public void setLastAlertAt(LocalDateTime lastAlertAt) {
        this.lastAlertAt = lastAlertAt;
    }
}
