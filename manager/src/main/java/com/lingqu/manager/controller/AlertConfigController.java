package com.lingqu.manager.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lingqu.manager.common.Result;
import com.lingqu.manager.entity.AlertConfig;
import com.lingqu.manager.service.AlertConfigService;
import com.lingqu.manager.service.PermService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 告警规则管理（需求 3.8.3）。
 */
@RestController
@RequestMapping("/api/admin/alerts")
public class AlertConfigController {

    private final AlertConfigService alertConfigService;
    private final PermService permService;

    public AlertConfigController(AlertConfigService alertConfigService, PermService permService) {
        this.alertConfigService = alertConfigService;
        this.permService = permService;
    }

    @GetMapping
    public Result<IPage<AlertConfig>> page(@RequestParam(defaultValue = "1") long page,
                                           @RequestParam(defaultValue = "10") long size,
                                           @RequestParam(required = false) String keyword) {
        permService.requireAdmin();
        return Result.ok(alertConfigService.page(page, size, keyword));
    }

    @GetMapping("/all")
    public Result<List<AlertConfig>> list() {
        permService.requireAdmin();
        return Result.ok(alertConfigService.list());
    }

    @GetMapping("/{id}")
    public Result<AlertConfig> get(@PathVariable String id) {
        permService.requireAdmin();
        return Result.ok(alertConfigService.get(id));
    }

    @PostMapping
    public Result<Void> create(@RequestBody AlertConfig config) {
        permService.requireAdmin();
        alertConfigService.create(config);
        return Result.ok();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable String id, @RequestBody AlertConfig config) {
        permService.requireAdmin();
        config.setId(id);
        alertConfigService.update(config);
        return Result.ok();
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable String id, @RequestBody Map<String, Integer> body) {
        permService.requireAdmin();
        alertConfigService.updateStatus(id, body.get("status"));
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        permService.requireAdmin();
        alertConfigService.delete(id);
        return Result.ok();
    }
}
