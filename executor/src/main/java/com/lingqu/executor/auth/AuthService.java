package com.lingqu.executor.auth;

import com.lingqu.executor.cache.ConfigCache;
import com.lingqu.executor.common.BizException;
import com.lingqu.executor.common.CryptoUtil;
import com.lingqu.executor.entity.Project;
import com.lingqu.executor.entity.Token;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 路由识别 + 项目鉴权（需求 3.4.4 / 3.4.5）。
 */
@Component
public class AuthService {

    private final ConfigCache configCache;

    public AuthService(ConfigCache configCache) {
        this.configCache = configCache;
    }

    /**
     * 从完整请求路径中提取第一段作为项目 route_prefix 并查找项目。
     * 如 /api/order/getDetail → 前缀 /api/order。
     */
    public Project resolveProject(String fullPath) {
        if (fullPath == null || !fullPath.startsWith("/")) {
            return null;
        }
        int idx = fullPath.indexOf('/', 1);
        String prefix = idx > 0 ? fullPath.substring(0, idx) : fullPath;
        return configCache.findProject(prefix);
    }

    /** 提取接口路径（前缀之后的部分），如 /api/order/getDetail → /getDetail */
    public String resolveApiPath(String fullPath) {
        if (fullPath == null) {
            return null;
        }
        int idx = fullPath.indexOf('/', 1);
        if (idx < 0) {
            return null;
        }
        String rest = fullPath.substring(idx);
        return rest.isEmpty() ? null : rest;
    }

    /** 校验项目存在且启用；不存在/禁用统一返回 404 避免泄露信息 */
    public void checkProject(Project project) {
        if (project == null) {
            throw new BizException(404, "接口不存在");
        }
        if (project.getStatus() == null || project.getStatus() == 0) {
            throw new BizException(404, "接口不存在");
        }
    }

    /**
     * 项目级鉴权：
     *   none    - 不鉴权
     *   token   - Authorization: Bearer xxx
     *   apikey  - X-API-Key: xxx
     */
    public void checkAuth(Project project, String authorization, String apiKey) {
        String type = project.getAuthType() == null ? "none" : project.getAuthType();
        switch (type) {
            case "token":
                if (authorization == null || !authorization.startsWith("Bearer ")) {
                    throw new BizException(401, "缺少 Bearer Token");
                }
                String token = authorization.substring(7).trim();
                if (!validToken(project.getId(), token)) {
                    throw new BizException(401, "Token 无效或已过期");
                }
                break;
            case "apikey":
                if (apiKey == null || apiKey.isEmpty()) {
                    throw new BizException(401, "缺少 X-API-Key 请求头");
                }
                if (!validToken(project.getId(), apiKey.trim())) {
                    throw new BizException(401, "API Key 无效或已过期");
                }
                break;
            case "none":
            default:
                break;
        }
    }

    /** 库中存储的是 AES 加密值，比对时对传入明文做同样加密后匹配（需求 4.5.4 严格校验 Token 与项目绑定） */
    private boolean validToken(String projectId, String plainToken) {
        String encrypted = CryptoUtil.encrypt(plainToken);
        for (Token t : configCache.getTokens(projectId)) {
            if (t.getStatus() != null && t.getStatus() == 1
                    && encrypted.equals(t.getToken())
                    && (t.getExpireAt() == null || t.getExpireAt().isAfter(LocalDateTime.now()))) {
                return true;
            }
        }
        return false;
    }
}
