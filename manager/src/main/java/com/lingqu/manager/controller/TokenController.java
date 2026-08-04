package com.lingqu.manager.controller;

import com.lingqu.manager.common.Result;
import com.lingqu.manager.entity.Token;
import com.lingqu.manager.service.TokenService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Token 管理接口。
 */
@RestController
@RequestMapping("/api/admin/tokens")
public class TokenController {

    private final TokenService tokenService;

    public TokenController(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @GetMapping
    public Result<List<Token>> list(@RequestParam(required = false) String projectId) {
        return Result.ok(tokenService.list(projectId));
    }

    @PostMapping
    public Result<Token> create(@RequestBody Map<String, Object> body) {
        String projectId = (String) body.get("projectId");
        String tokenName = (String) body.get("tokenName");
        Integer expireDays = body.get("expireDays") == null ? null : Integer.valueOf(body.get("expireDays").toString());
        return Result.ok(tokenService.create(projectId, tokenName, expireDays));
    }

    @DeleteMapping("/{id}")
    public Result<Void> revoke(@PathVariable String id) {
        tokenService.revoke(id);
        return Result.ok();
    }
}
