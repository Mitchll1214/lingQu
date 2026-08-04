package com.lingqu.manager.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lingqu.manager.common.Result;
import com.lingqu.manager.entity.ApiLog;
import com.lingqu.manager.service.LogService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 调用日志查询接口。
 */
@RestController
@RequestMapping("/api/admin/logs")
public class LogController {

    private final LogService logService;

    public LogController(LogService logService) {
        this.logService = logService;
    }

    @GetMapping
    public Result<IPage<ApiLog>> page(@RequestParam(defaultValue = "1") long page,
                                      @RequestParam(defaultValue = "10") long size,
                                      @RequestParam(required = false) String projectId,
                                      @RequestParam(required = false) String apiId,
                                      @RequestParam(required = false)
                                      @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start,
                                      @RequestParam(required = false)
                                      @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end,
                                      @RequestParam(required = false) Integer success) {
        return Result.ok(logService.page(page, size, projectId, apiId, start, end, success));
    }
}
