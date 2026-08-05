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
     * 从完整请求路径中匹配项目 route_prefix（最长前缀优先）。
     * 如 /api/order/getDetail，route_prefix=/api/order 时命中，接口路径为 /getDetail。
     * 支持多段前缀（如 /api/test），并校验路径段边界（/api 不会误匹配 /apixxx）。
     */
    public Project resolveProject(String fullPath) {
        if (fullPath == null || !fullPath.startsWith("/")) {
            return null;
        }
        Project best = null;
        int bestLen = -1;
        for (Project p : configCache.allProjects()) {
            String prefix = p.getRoutePrefix();
            if (prefix == null || prefix.isEmpty() || prefix.charAt(0) != '/') {
                continue;
            }
            if (fullPath.startsWith(prefix)
                    && (fullPath.length() == prefix.length() || fullPath.charAt(prefix.length()) == '/')) {
                if (prefix.length() > bestLen) {
                    best = p;
                    bestLen = prefix.length();
                }
            }
        }
        return best;
    }

    /** 提取接口路径（匹配到的前缀之后的部分），如 /api/order/getDetail → /getDetail */
    public String resolveApiPath(Project project, String fullPath) {
        if (project == null || project.getRoutePrefix() == null || fullPath == null) {
            return null;
        }
        String rest = fullPath.substring(project.getRoutePrefix().length());
        return rest.startsWith("/") && !rest.equals("/") ? rest : null;
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
        LocalDateTime now = LocalDateTime.now();
        for (Token t : configCache.getTokens(projectId)) {
            if (t.getStatus() != null && t.getStatus() == 1
                    && encrypted.equals(t.getToken())) {
                // 有效期校验：开始时间（未到不生效）+ 结束时间（过期失效）
                if (t.getStartAt() != null && now.isBefore(t.getStartAt())) {
                    continue;
                }
                if (t.getExpireAt() != null && now.isAfter(t.getExpireAt())) {
                    continue;
                }
                return true;
            }
        }
        return false;
    }
}
