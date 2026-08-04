package com.lingqu.manager.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lingqu.manager.common.Result;
import com.lingqu.manager.entity.Api;
import com.lingqu.manager.service.ApiService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 接口管理接口。
 */
@RestController
@RequestMapping("/api/admin/apis")
public class ApiController {

    private final ApiService apiService;

    public ApiController(ApiService apiService) {
        this.apiService = apiService;
    }

    @GetMapping
    public Result<IPage<Api>> page(@RequestParam(defaultValue = "1") long page,
                                   @RequestParam(defaultValue = "10") long size,
                                   @RequestParam(required = false) String projectId,
                                   @RequestParam(required = false) String keyword,
                                   @RequestParam(required = false) Integer status) {
        return Result.ok(apiService.page(page, size, projectId, keyword, status));
    }

    @GetMapping("/{id}")
    public Result<Api> get(@PathVariable String id) {
        return Result.ok(apiService.get(id));
    }

    @PostMapping
    public Result<Void> create(@RequestBody Api api) {
        apiService.create(api);
        return Result.ok();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable String id, @RequestBody Api api) {
        api.setId(id);
        apiService.update(api);
        return Result.ok();
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable String id, @RequestBody Map<String, Integer> body) {
        apiService.updateStatus(id, body.get("status"));
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        apiService.delete(id);
        return Result.ok();
    }
}
