package com.lingqu.manager.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lingqu.manager.common.Result;
import com.lingqu.manager.entity.Project;
import com.lingqu.manager.service.ProjectService;
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
 * 项目管理接口。
 */
@RestController
@RequestMapping("/api/admin/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public Result<IPage<Project>> page(@RequestParam(defaultValue = "1") long page,
                                       @RequestParam(defaultValue = "10") long size,
                                       @RequestParam(required = false) String keyword,
                                       @RequestParam(required = false) Integer status) {
        return Result.ok(projectService.page(page, size, keyword, status));
    }

    @GetMapping("/options")
    public Result<List<Project>> options() {
        return Result.ok(projectService.options());
    }

    @GetMapping("/{id}")
    public Result<Project> get(@PathVariable String id) {
        return Result.ok(projectService.get(id));
    }

    @PostMapping
    public Result<Void> create(@RequestBody Project project) {
        projectService.create(project);
        return Result.ok();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable String id, @RequestBody Project project) {
        project.setId(id);
        projectService.update(project);
        return Result.ok();
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable String id, @RequestBody Map<String, Integer> body) {
        projectService.updateStatus(id, body.get("status"));
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        projectService.delete(id);
        return Result.ok();
    }
}
