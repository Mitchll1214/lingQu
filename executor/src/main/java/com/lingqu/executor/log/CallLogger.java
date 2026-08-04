package com.lingqu.executor.log;

import com.lingqu.executor.common.IdUtil;
import com.lingqu.executor.entity.Api;
import com.lingqu.executor.entity.ApiLog;
import com.lingqu.executor.entity.Project;
import com.lingqu.executor.mapper.ApiLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 调用日志写入（需求 3.5）。日志存储于外部配置库 lingqu_api_log 表。
 */
@Component
public class CallLogger {

    private static final Logger log = LoggerFactory.getLogger(CallLogger.class);

    private final ApiLogMapper apiLogMapper;

    public CallLogger(ApiLogMapper apiLogMapper) {
        this.apiLogMapper = apiLogMapper;
    }

    public void write(Project project, Api api, String requestPath, String requestMethod,
                      String requestParams, String responseData, int statusCode,
                      long costTime, String clientIp, String errorMsg) {
        ApiLog apiLog = new ApiLog();
        apiLog.setId(IdUtil.uuid());
        apiLog.setProjectId(project.getId());
        apiLog.setProjectCode(project.getCode());
        apiLog.setApiId(api.getId());
        apiLog.setApiName(api.getApiName());
        apiLog.setRequestPath(requestPath);
        apiLog.setRequestMethod(requestMethod);
        apiLog.setRequestParams(truncate(requestParams, 16000));
        apiLog.setResponseData(truncate(responseData, 16000));
        apiLog.setStatusCode(statusCode);
        apiLog.setCostTime(costTime);
        apiLog.setClientIp(clientIp);
        apiLog.setErrorMsg(truncate(errorMsg, 1000));
        apiLog.setCreatedAt(LocalDateTime.now());
        try {
            apiLogMapper.insert(apiLog);
        } catch (Exception e) {
            // 日志写入失败不影响业务响应
            log.warn("调用日志写入失败: {}", e.getMessage());
        }
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
