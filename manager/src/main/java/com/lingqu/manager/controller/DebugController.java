package com.lingqu.manager.controller;

import com.lingqu.manager.common.Result;
import com.lingqu.manager.service.DebugService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 在线调试（需求 3.7.2）。
 */
@RestController
@RequestMapping("/api/admin/debug")
public class DebugController {

    private final DebugService debugService;

    public DebugController(DebugService debugService) {
        this.debugService = debugService;
    }

    @PostMapping("/execute")
    public Result<Map<String, Object>> execute(@RequestBody Map<String, Object> body) {
        String projectId = (String) body.get("projectId");
        String apiId = (String) body.get("apiId");
        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) body.get("params");
        @SuppressWarnings("unchecked")
        Map<String, String> headers = (Map<String, String>) body.get("headers");
        return Result.ok(debugService.execute(projectId, apiId, params, headers));
    }
}
