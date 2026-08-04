package com.lingqu.manager.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lingqu.manager.common.BizException;
import com.lingqu.manager.config.AuthInterceptor;
import com.lingqu.manager.entity.User;
import com.lingqu.manager.mapper.UserMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;

/**
 * 登录认证。
 */
@Service
public class AuthService {

    private final UserMapper userMapper;

    public AuthService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public User login(String username, String password, HttpSession session) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        if (user == null || !new BCryptPasswordEncoder().matches(password, user.getPasswordHash())) {
            throw new BizException(401, "用户名或密码错误");
        }
        user.setPasswordHash(null);
        session.setAttribute(AuthInterceptor.SESSION_USER, user);
        return user;
    }

    public void logout(HttpSession session) {
        session.invalidate();
    }

    public User me(HttpSession session) {
        User user = (User) session.getAttribute(AuthInterceptor.SESSION_USER);
        if (user == null) {
            throw new BizException(401, "未登录");
        }
        return user;
    }
}
