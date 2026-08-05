package com.lingqu.manager.controller;

import com.lingqu.manager.common.Result;
import com.lingqu.manager.entity.Token;
import com.lingqu.manager.service.PermService;
import com.lingqu.manager.service.TokenService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Token 管理接口。
 */
@RestController
@RequestMapping("/api/admin/tokens")
public class TokenController {

    private final TokenService tokenService;
    private final PermService permService;

    public TokenController(TokenService tokenService, PermService permService) {
        this.tokenService = tokenService;
        this.permService = permService;
    }

    @GetMapping
    public Result<List<Token>> list(@RequestParam(required = false) String projectId) {
        return Result.ok(tokenService.list(projectId));
    }

    @PostMapping
    public Result<Token> create(@RequestBody Map<String, Object> body) {
        String projectId = (String) body.get("projectId");
        String tokenName = (String) body.get("tokenName");
        LocalDateTime startAt = parseTime(body.get("startAt"));
        LocalDateTime expireAt = parseTime(body.get("expireAt"));
        return Result.ok(tokenService.create(projectId, tokenName, startAt, expireAt));
    }

    /** 查看 Token 明文（主动操作，需项目权限） */
    @GetMapping("/{id}/reveal")
    public Result<String> reveal(@PathVariable String id) {
        return Result.ok(tokenService.reveal(id));
    }

    @DeleteMapping("/{id}")
    public Result<Void> revoke(@PathVariable String id) {
        tokenService.revoke(id);
        return Result.ok();
    }

    private LocalDateTime parseTime(Object value) {
        if (value == null || String.valueOf(value).isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(String.valueOf(value).replace('T', ' ').replaceFirst(" ", "T"));
    }
}
