package com.lingqu.executor.controller;

import com.lingqu.executor.cache.ConfigCache;
import com.lingqu.executor.common.Result;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 容器内部接口（仅用于 Manager 在线调试等场景，不对外提供业务能力）。
 * POST /internal/config/refresh：立即从配置库重载项目/接口/Token/数据源配置，
 * 消除「新建/上线接口后最多 30 秒生效」的缓存窗口。
 */
@RestController
@RequestMapping("/internal")
public class InternalController {

    private final ConfigCache configCache;

    public InternalController(ConfigCache configCache) {
        this.configCache = configCache;
    }

    @PostMapping("/config/refresh")
    public Result<Void> refreshConfig() {
        configCache.refresh();
        return Result.ok();
    }
}
