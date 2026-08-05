package com.lingqu.manager.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lingqu.manager.common.BizException;
import com.lingqu.manager.common.CurrentUser;
import com.lingqu.manager.common.IdUtil;
import com.lingqu.manager.entity.ProjectUser;
import com.lingqu.manager.entity.User;
import com.lingqu.manager.mapper.ProjectUserMapper;
import com.lingqu.manager.mapper.UserMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户管理：
 *   - 管理员：创建用户（初始密码 88888888）、绑定项目权限、重置密码（88888888）、启用/禁用
 *   - 所有用户：可修改自己的密码
 * 管理员账号（由环境变量 DEFAULT_ADMIN_USER/PASS 固定）受保护，不可被禁用/重置。
 */
@Service
public class UserService {

    /** 管理员重置用户密码 / 新建用户的默认密码 */
    public static final String DEFAULT_PASSWORD = "88888888";

    private final UserMapper userMapper;
    private final ProjectUserMapper projectUserMapper;
    private final PermService permService;

    public UserService(UserMapper userMapper, ProjectUserMapper projectUserMapper, PermService permService) {
        this.userMapper = userMapper;
        this.projectUserMapper = projectUserMapper;
        this.permService = permService;
    }

    public IPage<User> page(long page, long size, String keyword) {
        permService.requireAdmin();
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.like(User::getUsername, keyword);
        }
        qw.orderByAsc(User::getCreatedAt);
        IPage<User> result = userMapper.selectPage(new Page<>(page, size), qw);
        result.getRecords().forEach(u -> u.setPasswordHash(null));
        return result;
    }

    /** 管理员创建用户：初始密码默认 88888888，角色 USER */
    public void create(String username, String password) {
        permService.requireAdmin();
        if (!StringUtils.hasText(username)) {
            throw new BizException("用户名不能为空");
        }
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, username.trim()));
        if (exists != null && exists > 0) {
            throw new BizException("用户名已存在：" + username);
        }
        User user = new User();
        user.setId(IdUtil.uuid());
        user.setUsername(username.trim());
        user.setPasswordHash(new BCryptPasswordEncoder().encode(StringUtils.hasText(password) ? password : DEFAULT_PASSWORD));
        user.setRole(User.ROLE_USER);
        user.setStatus(1);
        userMapper.insert(user);
    }

    /** 查询用户已绑定的项目 ID 列表（管理员） */
    public List<String> userProjects(String userId) {
        permService.requireAdmin();
        return projectUserMapper.selectList(new LambdaQueryWrapper<ProjectUser>()
                        .eq(ProjectUser::getUserId, userId))
                .stream().map(ProjectUser::getProjectId).collect(Collectors.toList());
    }

    /** 管理员设置用户的项目权限（全量覆盖） */
    public void updateProjects(String userId, List<String> projectIds) {
        permService.requireAdmin();
        if (userMapper.selectById(userId) == null) {
            throw new BizException(404, "用户不存在");
        }
        projectUserMapper.delete(new LambdaQueryWrapper<ProjectUser>().eq(ProjectUser::getUserId, userId));
        if (projectIds != null) {
            for (String projectId : projectIds) {
                if (!StringUtils.hasText(projectId)) {
                    continue;
                }
                ProjectUser pu = new ProjectUser();
                pu.setId(IdUtil.uuid());
                pu.setUserId(userId);
                pu.setProjectId(projectId);
                projectUserMapper.insert(pu);
            }
        }
    }

    /** 管理员重置用户密码为默认 88888888（admin 账号受保护） */
    public void resetPassword(String userId) {
        permService.requireAdmin();
        User user = getUser(userId);
        if (User.ROLE_ADMIN.equals(user.getRole()) || "admin".equals(user.getUsername())) {
            throw new BizException("管理员账号不可被重置，请通过环境变量 DEFAULT_ADMIN_PASS 修改");
        }
        user.setPasswordHash(new BCryptPasswordEncoder().encode(DEFAULT_PASSWORD));
        userMapper.updateById(user);
    }

    /** 管理员启用/禁用用户（admin 账号受保护） */
    public void updateStatus(String userId, Integer status) {
        permService.requireAdmin();
        if (status == null || (status != 0 && status != 1)) {
            throw new BizException("状态值只能为 0（禁用）或 1（启用）");
        }
        User user = getUser(userId);
        if (User.ROLE_ADMIN.equals(user.getRole()) || "admin".equals(user.getUsername())) {
            throw new BizException("管理员账号不可被禁用");
        }
        User patch = new User();
        patch.setId(userId);
        patch.setStatus(status);
        userMapper.updateById(patch);
    }

    /** 当前用户修改自己的密码 */
    public void changePassword(String oldPassword, String newPassword) {
        String userId = CurrentUser.id();
        if (userId == null) {
            throw new BizException(401, "未登录");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(404, "用户不存在");
        }
        if (!new BCryptPasswordEncoder().matches(oldPassword, user.getPasswordHash())) {
            throw new BizException("原密码不正确");
        }
        if (!StringUtils.hasText(newPassword) || newPassword.length() < 6) {
            throw new BizException("新密码长度不能少于 6 位");
        }
        user.setPasswordHash(new BCryptPasswordEncoder().encode(newPassword));
        userMapper.updateById(user);
    }

    private User getUser(String userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(404, "用户不存在");
        }
        return user;
    }
}
