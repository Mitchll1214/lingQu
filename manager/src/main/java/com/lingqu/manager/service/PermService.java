package com.lingqu.manager.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lingqu.manager.common.BizException;
import com.lingqu.manager.common.CurrentUser;
import com.lingqu.manager.entity.ProjectUser;
import com.lingqu.manager.mapper.ProjectUserMapper;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 项目权限服务：
 *   - 管理员：全部项目可访问
 *   - 普通用户：仅可访问 lingqu_project_user 中绑定给自己的项目
 */
@Component
public class PermService {

    private final ProjectUserMapper projectUserMapper;

    public PermService(ProjectUserMapper projectUserMapper) {
        this.projectUserMapper = projectUserMapper;
    }

    /**
     * 当前用户可访问的项目 ID 列表；管理员返回 null 表示全部。
     */
    public List<String> permittedProjectIds() {
        if (CurrentUser.isAdmin()) {
            return null;
        }
        String userId = CurrentUser.id();
        if (userId == null) {
            throw new BizException(401, "未登录");
        }
        List<ProjectUser> list = projectUserMapper.selectList(new LambdaQueryWrapper<ProjectUser>()
                .eq(ProjectUser::getUserId, userId));
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        return list.stream().map(ProjectUser::getProjectId).collect(Collectors.toList());
    }

    /** 校验当前用户对指定项目有访问权（管理员直接放行） */
    public void checkProjectPermission(String projectId) {
        if (CurrentUser.isAdmin()) {
            return;
        }
        String userId = CurrentUser.id();
        if (userId == null) {
            throw new BizException(401, "未登录");
        }
        Long count = projectUserMapper.selectCount(new LambdaQueryWrapper<ProjectUser>()
                .eq(ProjectUser::getUserId, userId)
                .eq(ProjectUser::getProjectId, projectId));
        if (count == null || count == 0) {
            throw new BizException(403, "无权访问该项目");
        }
    }

    /** 仅管理员可执行 */
    public void requireAdmin() {
        if (!CurrentUser.isAdmin()) {
            throw new BizException(403, "仅管理员可执行此操作");
        }
    }
}
