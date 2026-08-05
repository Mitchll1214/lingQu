package com.lingqu.executor.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingqu.executor.auth.AuthService;
import com.lingqu.executor.cache.ConfigCache;
import com.lingqu.executor.common.BizException;
import com.lingqu.executor.engine.GroovyExecutor;
import com.lingqu.executor.engine.RateLimiterManager;
import com.lingqu.executor.engine.ResponseFormatter;
import com.lingqu.executor.engine.SqlExecutor;
import com.lingqu.executor.entity.Api;
import com.lingqu.executor.entity.Project;
import com.lingqu.executor.log.CallLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 业务 API 分发入口（catch-all）。
 * 职责：路由识别 → 项目校验 → 接口校验 → 鉴权 → 限流 → SQL/Groovy 执行 → 调用日志。
 * 完整调用路径格式：/{项目route_prefix}/{接口api_path}
 */
@RestController
public class ApiDispatchController {

    private static final Logger log = LoggerFactory.getLogger(ApiDispatchController.class);

    private final AuthService authService;
    private final ConfigCache configCache;
    private final SqlExecutor sqlExecutor;
    private final GroovyExecutor groovyExecutor;
    private final RateLimiterManager rateLimiterManager;
    private final ResponseFormatter responseFormatter;
    private final CallLogger callLogger;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ApiDispatchController(AuthService authService, ConfigCache configCache,
                                 SqlExecutor sqlExecutor, GroovyExecutor groovyExecutor,
                                 RateLimiterManager rateLimiterManager,
                                 ResponseFormatter responseFormatter, CallLogger callLogger) {
        this.authService = authService;
        this.configCache = configCache;
        this.sqlExecutor = sqlExecutor;
        this.groovyExecutor = groovyExecutor;
        this.rateLimiterManager = rateLimiterManager;
        this.responseFormatter = responseFormatter;
        this.callLogger = callLogger;
    }

    @RequestMapping(value = "/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public Object dispatch(HttpServletRequest request) {
        String fullPath = normalizePath(request);
        long start = System.currentTimeMillis();

        Project project = null;
        Api api = null;
        int statusCode = 500;
        String errorMsg = null;
        String responseBody = null;
        String paramsJson = null;

        try {
            project = authService.resolveProject(fullPath);
            authService.checkProject(project);

            String apiPath = authService.resolveApiPath(project, fullPath);
            api = configCache.findApi(project.getId(), apiPath);
            if (api == null || !request.getMethod().equalsIgnoreCase(api.getMethod())) {
                throw new BizException(404, "接口不存在");
            }
            if (api.getStatus() == null || api.getStatus() != Api.STATUS_ONLINE) {
                throw new BizException(404, "接口不存在");
            }

            // 项目级鉴权
            authService.checkAuth(project,
                    request.getHeader("Authorization"),
                    request.getHeader("X-API-Key"));

            // 接口级限流
            rateLimiterManager.acquire(api);

            // 请求参数：query 参数 + JSON body 合并
            Map<String, Object> params = collectParams(request);
            paramsJson = toJson(params);

            // 系统内置参数（自动注入，无需用户传参）：
            //   requestTime      当前时间，格式 yyyy-MM-dd HH:mm:ss（容器时区）
            //   requestTimeMillis 当前时间毫秒时间戳
            params.put("requestTime", java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            params.put("requestTimeMillis", System.currentTimeMillis());
            paramsJson = toJson(params);

            // 执行
            Object data;
            if ("groovy".equalsIgnoreCase(api.getSqlType())) {
                data = groovyExecutor.execute(project.getDatasourceId(), api, params);
            } else {
                data = sqlExecutor.execute(project.getDatasourceId(), api, params);
            }
            // 出参映射（需求 3.3.8）：字段重命名/格式化
            data = responseFormatter.format(api, data);
            responseBody = toJson(data);
            statusCode = 200;
            return data;
        } catch (BizException e) {
            statusCode = e.getCode();
            errorMsg = e.getMessage();
            throw e;
        } finally {
            long cost = System.currentTimeMillis() - start;
            // 仅开启日志开关的接口记录（需求 3.5.1）
            if (project != null && api != null
                    && api.getLogEnabled() != null && api.getLogEnabled() == 1) {
                callLogger.write(project, api, fullPath, request.getMethod(),
                        paramsJson, responseBody, statusCode, cost, clientIp(request), errorMsg);
            }
        }
    }

    private String normalizePath(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        return path;
    }

    private Map<String, Object> collectParams(HttpServletRequest request) {
        Map<String, Object> params = new HashMap<>();
        request.getParameterMap().forEach((k, v) -> params.put(k, v.length > 0 ? v[0] : ""));
        String method = request.getMethod();
        if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) {
            String contentType = request.getContentType();
            if (contentType != null && contentType.toLowerCase().contains("application/json")) {
                try {
                    String body = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                    if (!body.trim().isEmpty()) {
                        Object parsed = objectMapper.readValue(body, Object.class);
                        if (parsed instanceof Map) {
                            ((Map<?, ?>) parsed).forEach((k, v) -> params.put(String.valueOf(k), v));
                        } else {
                            params.put("body", parsed);
                        }
                    }
                } catch (BizException e) {
                    throw e;
                } catch (Exception e) {
                    throw new BizException(400, "请求体不是合法的 JSON");
                }
            }
        }
        return params;
    }

    private String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return String.valueOf(obj);
        }
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
