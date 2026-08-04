package com.lingqu.manager.config;

import com.lingqu.manager.common.BizException;
import com.lingqu.manager.entity.User;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * 管理后台登录拦截器：所有 /api/admin/** 请求（除登录）需已登录。
 * 使用 Servlet 原生 Session，登录态保存在服务端。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String SESSION_USER = "loginUser";

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
        return true;
    }
}
