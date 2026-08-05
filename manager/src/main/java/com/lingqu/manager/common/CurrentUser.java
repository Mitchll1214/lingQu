package com.lingqu.manager.common;

import com.lingqu.manager.config.AuthInterceptor;
import com.lingqu.manager.entity.User;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * 当前登录用户工具（从 Session 读取，由 AuthInterceptor 维护）。
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static User get() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        HttpServletRequest request = attrs.getRequest();
        HttpSession session = request.getSession(false);
        return session == null ? null : (User) session.getAttribute(AuthInterceptor.SESSION_USER);
    }

    public static String id() {
        User user = get();
        return user == null ? null : user.getId();
    }

    public static boolean isAdmin() {
        User user = get();
        return user != null && User.ROLE_ADMIN.equals(user.getRole());
    }
}
