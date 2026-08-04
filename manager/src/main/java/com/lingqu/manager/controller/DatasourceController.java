package com.lingqu.manager.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lingqu.manager.common.Result;
import com.lingqu.manager.entity.Datasource;
import com.lingqu.manager.service.DatasourceService;
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
 * 数据源管理接口。
 */
@RestController
@RequestMapping("/api/admin/datasources")
public class DatasourceController {

    private final DatasourceService datasourceService;

    public DatasourceController(DatasourceService datasourceService) {
        this.datasourceService = datasourceService;
    }

    @GetMapping
    public Result<IPage<Datasource>> page(@RequestParam(defaultValue = "1") long page,
                                          @RequestParam(defaultValue = "10") long size,
                                          @RequestParam(required = false) String keyword) {
        return Result.ok(datasourceService.page(page, size, keyword));
    }

    @GetMapping("/options")
    public Result<List<Datasource>> options() {
        return Result.ok(datasourceService.options());
    }

    @GetMapping("/{id}")
    public Result<Datasource> get(@PathVariable String id) {
        Datasource ds = datasourceService.get(id);
        ds.setPassword(null);
        return Result.ok(ds);
    }

    @PostMapping
    public Result<Void> create(@RequestBody Datasource datasource) {
        datasourceService.create(datasource);
        return Result.ok();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable String id, @RequestBody Datasource datasource) {
        datasource.setId(id);
        datasourceService.update(datasource);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        datasourceService.delete(id);
        return Result.ok();
    }

    @PostMapping("/{id}/test")
    public Result<String> test(@PathVariable String id) {
        return Result.ok(datasourceService.test(id));
    }
}
