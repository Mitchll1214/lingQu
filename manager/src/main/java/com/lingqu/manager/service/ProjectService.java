package com.lingqu.manager.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lingqu.manager.common.BizException;
import com.lingqu.manager.common.IdUtil;
import com.lingqu.manager.entity.Api;
import com.lingqu.manager.entity.Datasource;
import com.lingqu.manager.entity.Project;
import com.lingqu.manager.mapper.ApiMapper;
import com.lingqu.manager.mapper.DatasourceMapper;
import com.lingqu.manager.mapper.ProjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

/**
 * 项目管理。
 */
@Service
public class ProjectService {

    private final ProjectMapper projectMapper;
    private final ApiMapper apiMapper;
    private final DatasourceMapper datasourceMapper;
    private final PermService permService;

    public ProjectService(ProjectMapper projectMapper, ApiMapper apiMapper,
                          DatasourceMapper datasourceMapper, PermService permService) {
        this.projectMapper = projectMapper;
        this.apiMapper = apiMapper;
        this.datasourceMapper = datasourceMapper;
        this.permService = permService;
    }

    public IPage<Project> page(long page, long size, String keyword, Integer status) {
        LambdaQueryWrapper<Project> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(Project::getName, keyword).or().like(Project::getCode, keyword));
        }
        if (status != null) {
            qw.eq(Project::getStatus, status);
        }
        // 普通用户仅可见有权限的项目
        List<String> permitted = permService.permittedProjectIds();
        if (permitted != null) {
            if (permitted.isEmpty()) {
                return new Page<>(page, size);
            }
            qw.in(Project::getId, permitted);
        }
        qw.orderByDesc(Project::getUpdatedAt);
        return projectMapper.selectPage(new Page<>(page, size), qw);
    }

    public Project get(String id) {
        permService.checkProjectPermission(id);
        Project project = projectMapper.selectById(id);
        if (project == null) {
            throw new BizException(404, "项目不存在");
        }
        return project;
    }

    public void create(Project project) {
        permService.requireAdmin();
        validateBase(project);
        project.setId(IdUtil.uuid());
        if (project.getStatus() == null) {
            project.setStatus(1);
        }
        projectMapper.insert(project);
    }

    public void update(Project project) {
        permService.requireAdmin();
        get(project.getId());
        validateBase(project);
        // 唯一性校验需排除自身
        checkUnique(project.getCode(), project.getRoutePrefix(), project.getId());
        // 更换数据源：校验新数据源存在且可用（进行中调用校验后续版本实现）
        checkDatasource(project.getDatasourceId());
        // 编辑项目本身不改变状态（接口级重新上线逻辑在 ApiService）
        // 清空前端回传的时间戳，避免覆盖数据库真实值（否则 Executor 缓存无法感知变更）
        project.setCreatedAt(null);
        project.setUpdatedAt(null);
        projectMapper.updateById(project);
    }

    public void updateStatus(String id, Integer status) {
        permService.requireAdmin();
        if (status == null || (status != 0 && status != 1)) {
            throw new BizException("状态值只能为 0（禁用）或 1（启用）");
        }
        Project project = new Project();
        project.setId(id);
        project.setStatus(status);
        int rows = projectMapper.updateById(project);
        if (rows == 0) {
            throw new BizException(404, "项目不存在");
        }
    }

    /**
     * 软删除：仅当项目下无已上线接口时允许。仅管理员可操作。
     */
    public void delete(String id) {
        permService.requireAdmin();
        get(id);
        Long online = apiMapper.selectCount(new LambdaQueryWrapper<Api>()
                .eq(Api::getProjectId, id)
                .eq(Api::getStatus, Api.STATUS_ONLINE));
        if (online != null && online > 0) {
            throw new BizException("项目下存在已上线接口，请先全部下线后再删除");
        }
        projectMapper.deleteById(id);
    }

    /** 下拉选项：当前用户可见的启用项目 */
    public List<Project> options() {
        LambdaQueryWrapper<Project> qw = new LambdaQueryWrapper<>();
        qw.eq(Project::getStatus, 1);
        List<String> permitted = permService.permittedProjectIds();
        if (permitted != null) {
            if (permitted.isEmpty()) {
                return Collections.emptyList();
            }
            qw.in(Project::getId, permitted);
        }
        qw.orderByAsc(Project::getCode);
        return projectMapper.selectList(qw);
    }

    private void validateBase(Project project) {
        if (!StringUtils.hasText(project.getName())) {
            throw new BizException("项目名称不能为空");
        }
        if (!StringUtils.hasText(project.getCode())) {
            throw new BizException("项目编码不能为空");
        }
        if (!StringUtils.hasText(project.getRoutePrefix()) || !project.getRoutePrefix().startsWith("/")) {
            throw new BizException("调用路径前缀不能为空且必须以 / 开头");
        }
        if (project.getRoutePrefix().length() > 100) {
            throw new BizException("调用路径前缀长度不能超过 100");
        }
        checkUnique(project.getCode(), project.getRoutePrefix(), project.getId());
        checkDatasource(project.getDatasourceId());
    }

    private void checkUnique(String code, String routePrefix, String excludeId) {
        Long codeCount = projectMapper.selectCount(new LambdaQueryWrapper<Project>()
                .eq(Project::getCode, code)
                .ne(StringUtils.hasText(excludeId), Project::getId, excludeId));
        if (codeCount != null && codeCount > 0) {
            throw new BizException("项目编码已存在：" + code);
        }
        Long routeCount = projectMapper.selectCount(new LambdaQueryWrapper<Project>()
                .eq(Project::getRoutePrefix, routePrefix)
                .ne(StringUtils.hasText(excludeId), Project::getId, excludeId));
        if (routeCount != null && routeCount > 0) {
            throw new BizException("调用路径前缀已被其他项目占用：" + routePrefix);
        }
    }

    private void checkDatasource(String datasourceId) {
        if (!StringUtils.hasText(datasourceId)) {
            throw new BizException("必须绑定一个数据源");
        }
        Datasource ds = datasourceMapper.selectById(datasourceId);
        if (ds == null) {
            throw new BizException("绑定的数据源不存在");
        }
        if (ds.getStatus() != null && ds.getStatus() == 0) {
            throw new BizException("绑定的数据源当前不可用");
        }
    }
}
