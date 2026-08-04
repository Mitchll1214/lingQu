package com.lingqu.manager.service;

import com.lingqu.manager.common.IdUtil;
import com.lingqu.manager.entity.User;
import com.lingqu.manager.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 首次启动自动创建默认管理员账号（可配置 DEFAULT_ADMIN_USER / DEFAULT_ADMIN_PASS）。
 */
@Component
public class AdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final UserMapper userMapper;

    @Value("${app.default-admin-user:admin}")
    private String defaultUser;

    @Value("${app.default-admin-pass:123456}")
    private String defaultPass;

    public AdminInitializer(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        Long count = userMapper.selectCount(null);
        if (count != null && count > 0) {
            return;
        }
        User user = new User();
        user.setId(IdUtil.uuid());
        user.setUsername(defaultUser);
        user.setPasswordHash(new BCryptPasswordEncoder().encode(defaultPass));
        user.setRole("ADMIN");
        userMapper.insert(user);
        log.info("已创建默认管理员账号：{}（请尽快修改密码）", defaultUser);
    }
}
