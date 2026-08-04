package com.lingqu.manager.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 告警规则表 lingqu_alert_config
 */
@TableName("lingqu_alert_config")
public class AlertConfig {

    /** 规则类型：响应超时 */
    public static final String TYPE_TIMEOUT = "timeout";
    /** 规则类型：错误率 */
    public static final String TYPE_ERROR_RATE = "error_rate";

    @TableId(type = IdType.INPUT)
    private String id;

    /** 规则名称 */
    private String name;

    /** 目标项目 ID，null 表示全局 */
    private String projectId;

    /** timeout / error_rate */
    private String alertType;

    /** timeout: 秒；error_rate: 百分比 */
    private BigDecimal threshold;

    /** 统计窗口（分钟），默认 5 */
    private Integer windowMinutes;

    /** 防抖静默（分钟），默认 10 */
    private Integer silenceMinutes;

    /** 收件人（逗号分隔），空则用全局默认 */
    private String mailTo;

    /** 0禁用 1启用 */
    private Integer status;

    /** 上次告警时间（防抖依据） */
    private LocalDateTime lastAlertAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
