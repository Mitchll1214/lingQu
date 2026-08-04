package com.lingqu.manager.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 接口表 lingqu_api
 */
@TableName("lingqu_api")
public class Api {

    /** 接口状态：草稿 */
    public static final int STATUS_DRAFT = 0;
    /** 接口状态：已上线 */
    public static final int STATUS_ONLINE = 1;
    /** 接口状态：已下线 */
    public static final int STATUS_OFFLINE = 2;

    @TableId(type = IdType.INPUT)
    private String id;

    private String projectId;

    private String apiName;

    /** 接口路径，项目内唯一，如 /getDetail */
    private String apiPath;

    /** GET/POST/PUT/DELETE */
    private String method;

    /** SQL 或 Groovy 脚本 */
    private String sqlContent;

    /** sql/groovy */
    private String sqlType;

    /** 入参定义（JSON 字符串） */
    private String params;

    /** 出参映射（JSON 字符串，可选） */
    private String responseFormat;

    /** 日志开关：0关闭 1开启 */
    private Integer logEnabled;

    /** QPS 限制，0 不限制 */
    private BigDecimal rateLimitQps;

    /** 0草稿 1上线 2下线 */
    private Integer status;

    private String version;

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

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getApiName() {
        return apiName;
    }

    public void setApiName(String apiName) {
        this.apiName = apiName;
    }

    public String getApiPath() {
        return apiPath;
    }

    public void setApiPath(String apiPath) {
        this.apiPath = apiPath;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getSqlContent() {
        return sqlContent;
    }

    public void setSqlContent(String sqlContent) {
        this.sqlContent = sqlContent;
    }

    public String getSqlType() {
        return sqlType;
    }

    public void setSqlType(String sqlType) {
        this.sqlType = sqlType;
    }

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }

    public String getResponseFormat() {
        return responseFormat;
    }

    public void setResponseFormat(String responseFormat) {
        this.responseFormat = responseFormat;
    }

    public Integer getLogEnabled() {
        return logEnabled;
    }

    public void setLogEnabled(Integer logEnabled) {
        this.logEnabled = logEnabled;
    }

    public BigDecimal getRateLimitQps() {
        return rateLimitQps;
    }

    public void setRateLimitQps(BigDecimal rateLimitQps) {
        this.rateLimitQps = rateLimitQps;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
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
