package com.lingqu.manager.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lingqu.manager.common.BizException;
import com.lingqu.manager.entity.User;
import com.lingqu.manager.mapper.UserMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * 管理后台登录拦截器：所有 /api/admin/** 请求（除登录）需已登录。
 * 使用 Servlet 原生 Session，登录态保存在服务端。
 * 周期性复查用户状态（每 30 秒一次），账号被禁用后现有会话会及时失效。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String SESSION_USER = "loginUser";
    private static final String SESSION_CHECK_AT = "lingqu_user_check_at";
    private static final long CHECK_INTERVAL_MS = 30_000L;

    private final UserMapper userMapper;

    public AuthInterceptor(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        HttpSession session = request.getSession(false);
        User user = session == null ? null : (User) session.getAttribute(SESSION_USER);
        if (user == null) {
            throw new BizException(401, "未登录或登录已过期");
        }
        // 周期性复查用户是否仍启用（管理员被禁用/删除后会话及时失效）
        Long lastCheck = (Long) session.getAttribute(SESSION_CHECK_AT);
        long now = System.currentTimeMillis();
        if (lastCheck == null || now - lastCheck > CHECK_INTERVAL_MS) {
            User db = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getId, user.getId()));
            if (db == null || db.getStatus() == null || db.getStatus() != 1) {
                session.invalidate();
                throw new BizException(401, "账号已被禁用或删除，请重新登录");
            }
            session.setAttribute(SESSION_CHECK_AT, now);
        }
        return true;
    }
}
