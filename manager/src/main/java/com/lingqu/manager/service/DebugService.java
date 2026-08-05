package com.lingqu.manager.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingqu.manager.common.BizException;
import com.lingqu.manager.entity.Api;
import com.lingqu.manager.entity.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * 在线调试（需求 3.7.2）：由 Manager 转发模拟请求到 Executor（localhost 直连），
 * 走完整真实链路（路由、鉴权、限流、SQL 执行）。
 * Executor 地址统一从环境变量 EXECUTOR_PORT / EXECUTOR_BASE_URL 获取，
 * 与 Executor 进程监听端口天然一致（避免 yml 嵌套占位符与容器端口不一致）。
 */
@Service
public class DebugService {

    private static final Logger log = LoggerFactory.getLogger(DebugService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ProjectService projectService;
    private final ApiService apiService;
    private final PermService permService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${EXECUTOR_PORT:8080}")
    private int executorPort;

    /** 高级场景可整体覆盖 Executor 地址（如跨主机），默认不配置 */
    @Value("${EXECUTOR_BASE_URL:}")
    private String executorBaseUrl;

    public DebugService(ProjectService projectService, ApiService apiService, PermService permService) {
        this.projectService = projectService;
        this.apiService = apiService;
        this.permService = permService;
    }

    /** 实际转发基础地址（供前端展示与内部转发使用） */
    public String getExecutorBaseUrl() {
        if (StringUtils.hasText(executorBaseUrl)) {
            return executorBaseUrl.replaceAll("/+$", "");
        }
        return "http://localhost:" + executorPort;
    }

    public Map<String, Object> execute(String projectId, String apiId, Map<String, Object> params,
                                       Map<String, String> headers) {
        permService.checkProjectPermission(projectId);
        Project project = projectService.get(projectId);
        Api api = apiService.get(apiId);
        if (!api.getProjectId().equals(projectId)) {
            throw new BizException("接口不属于该项目");
        }
        // 前置校验：未上线接口给出明确提示（Executor 对未上线接口统一返回 404，新手会困惑）
        if (api.getStatus() == null || api.getStatus() != Api.STATUS_ONLINE) {
            throw new BizException("该接口尚未上线，请先在「接口管理」中点击上线后再调试");
        }
        if (project.getStatus() == null || project.getStatus() == 0) {
            throw new BizException("该项目当前已禁用，无法调试");
        }

        String baseUrl = getExecutorBaseUrl();
        // 调试前触发 Executor 立即刷新配置，消除 30 秒缓存窗口（新建/上线接口后立即可调试）
        refreshExecutorConfig(baseUrl);
        String url = baseUrl + project.getRoutePrefix() + api.getApiPath();

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        if (headers != null) {
            headers.forEach((k, v) -> {
                if (StringUtils.hasText(k) && StringUtils.hasText(v)) {
                    httpHeaders.set(k, v);
                }
            });
        }

        HttpMethod method = HttpMethod.valueOf(api.getMethod());
        Object body = null;
        String target = url;
        if (method == HttpMethod.GET || method == HttpMethod.DELETE) {
            // 参数放入 query string
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
            if (params != null) {
                params.forEach((k, v) -> {
                    if (v != null) {
                        builder.queryParam(k, String.valueOf(v));
                    }
                });
            }
            target = builder.build().encode().toUriString();
        } else {
            body = params;
        }

        String bodyJson = null;
        if (body != null) {
            try {
                bodyJson = MAPPER.writeValueAsString(body);
            } catch (Exception e) {
                throw new BizException("请求参数序列化失败：" + e.getMessage());
            }
        }

        long start = System.currentTimeMillis();
        ResponseEntity<String> response;
        try {
            response = restTemplate.exchange(target, method, new HttpEntity<>(bodyJson, httpHeaders), String.class);
        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode().value() == 404) {
                throw new BizException(404, "接口不存在（转发地址 " + url + "）。请确认：接口已上线、项目/接口路径与请求方法正确。");
            }
            throw new BizException(500, "调试请求失败：" + e.getStatusCode().value() + " " + e.getResponseBodyAsString());
        } catch (RestClientException e) {
            throw new BizException(500, "调试请求失败（无法连接 Executor " + url + "）：" + e.getMessage());
        }
        long cost = System.currentTimeMillis() - start;

        Map<String, Object> result = new HashMap<>();
        result.put("status", response.getStatusCodeValue());
        result.put("body", response.getBody());
        result.put("costTime", cost);
        result.put("url", url);
        return result;
    }

    /** 触发 Executor 立即刷新项目/接口/Token 配置；失败不阻塞调试（30s 内也会自动刷新） */
    private void refreshExecutorConfig(String baseUrl) {
        try {
            restTemplate.postForEntity(baseUrl + "/internal/config/refresh", null, String.class);
        } catch (Exception e) {
            log.debug("Executor 配置刷新调用失败：{}", e.getMessage());
        }
    }
}
