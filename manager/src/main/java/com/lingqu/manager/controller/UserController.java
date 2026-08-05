package com.lingqu.manager.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lingqu.manager.common.Result;
import com.lingqu.manager.entity.User;
import com.lingqu.manager.service.UserService;
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
 * 用户管理（管理员）。
 */
@RestController
@RequestMapping("/api/admin/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public Result<IPage<User>> page(@RequestParam(defaultValue = "1") long page,
                                    @RequestParam(defaultValue = "10") long size,
                                    @RequestParam(required = false) String keyword) {
        return Result.ok(userService.page(page, size, keyword));
    }

    @PostMapping
    public Result<Void> create(@RequestBody Map<String, String> body) {
        userService.create(body.get("username"), body.get("password"));
        return Result.ok();
    }

    @GetMapping("/{id}/projects")
    public Result<List<String>> userProjects(@PathVariable String id) {
        return Result.ok(userService.userProjects(id));
    }

    @PutMapping("/{id}/projects")
    public Result<Void> updateProjects(@PathVariable String id, @RequestBody Map<String, List<String>> body) {
        userService.updateProjects(id, body.get("projectIds"));
        return Result.ok();
    }

    @PostMapping("/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable String id) {
        userService.resetPassword(id);
        return Result.ok();
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable String id, @RequestBody Map<String, Integer> body) {
        userService.updateStatus(id, body.get("status"));
        return Result.ok();
    }

    /** 当前用户修改自己的密码 */
    @PostMapping("/change-password")
    public Result<Void> changePassword(@RequestBody Map<String, String> body) {
        userService.changePassword(body.get("oldPassword"), body.get("newPassword"));
        return Result.ok();
    }
}
