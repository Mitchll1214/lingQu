package com.lingqu.manager.controller;

import com.lingqu.manager.common.Result;
import com.lingqu.manager.entity.User;
import com.lingqu.manager.service.AuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.Map;

/**
 * 登录认证。
 */
@RestController
@RequestMapping("/api/admin/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public Result<User> login(@RequestBody Map<String, String> body, HttpSession session) {
        String username = body.get("username");
        String password = body.get("password");
        return Result.ok(authService.login(username, password, session));
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpSession session) {
        authService.logout(session);
        return Result.ok();
    }

    @GetMapping("/me")
    public Result<User> me(HttpSession session) {
        return Result.ok(authService.me(session));
    }
}
