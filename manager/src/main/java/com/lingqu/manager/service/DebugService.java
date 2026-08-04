package com.lingqu.manager.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingqu.manager.common.BizException;
import com.lingqu.manager.entity.Api;
import com.lingqu.manager.entity.Project;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * 在线调试（需求 3.7.2）：由 Manager 转发模拟请求到 Executor（localhost 直连），
 * 走完整真实链路（路由、鉴权、限流、SQL 执行）。
 */
@Service
public class DebugService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ProjectService projectService;
    private final ApiService apiService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.executor-url:http://localhost:8080}")
    private String executorUrl;

    public DebugService(ProjectService projectService, ApiService apiService) {
        this.projectService = projectService;
        this.apiService = apiService;
    }

    public Map<String, Object> execute(String projectId, String apiId, Map<String, Object> params,
                                       Map<String, String> headers) {
        Project project = projectService.get(projectId);
        Api api = apiService.get(apiId);
        if (!api.getProjectId().equals(projectId)) {
            throw new BizException("接口不属于该项目");
        }

        String baseUrl = executorUrl.replaceAll("/+$", "") + project.getRoutePrefix() + api.getApiPath();
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
        String url = baseUrl;
        if (method == HttpMethod.GET || method == HttpMethod.DELETE) {
            // 参数放入 query string
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl);
            if (params != null) {
                params.forEach((k, v) -> {
                    if (v != null) {
                        builder.queryParam(k, String.valueOf(v));
                    }
                });
            }
            url = builder.build().encode().toUriString();
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
            response = restTemplate.exchange(url, method, new HttpEntity<>(bodyJson, httpHeaders), String.class);
        } catch (RestClientException e) {
            throw new BizException(500, "调试请求失败：" + e.getMessage());
        }
        long cost = System.currentTimeMillis() - start;

        Map<String, Object> result = new HashMap<>();
        result.put("status", response.getStatusCodeValue());
        result.put("body", response.getBody());
        result.put("costTime", cost);
        return result;
    }
}
