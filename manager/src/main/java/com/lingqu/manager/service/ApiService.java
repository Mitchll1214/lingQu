package com.lingqu.manager.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lingqu.manager.common.BizException;
import com.lingqu.manager.common.IdUtil;
import com.lingqu.manager.entity.Api;
import com.lingqu.manager.entity.Project;
import com.lingqu.manager.mapper.ApiMapper;
import com.lingqu.manager.mapper.ProjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 接口管理。
 */
@Service
public class ApiService {

    private static final Set<String> METHODS = new HashSet<>(Arrays.asList("GET", "POST", "PUT", "DELETE"));

    private final ApiMapper apiMapper;
    private final ProjectMapper projectMapper;

    public ApiService(ApiMapper apiMapper, ProjectMapper projectMapper) {
        this.apiMapper = apiMapper;
        this.projectMapper = projectMapper;
    }

    public IPage<Api> page(long page, long size, String projectId, String keyword, Integer status) {
        LambdaQueryWrapper<Api> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(projectId)) {
            qw.eq(Api::getProjectId, projectId);
        }
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(Api::getApiName, keyword).or().like(Api::getApiPath, keyword));
        }
        if (status != null) {
            qw.eq(Api::getStatus, status);
        }
        qw.orderByDesc(Api::getUpdatedAt);
        return apiMapper.selectPage(new Page<>(page, size), qw);
    }

    public Api get(String id) {
        Api api = apiMapper.selectById(id);
        if (api == null) {
            throw new BizException(404, "接口不存在");
        }
        return api;
    }

    public void create(Api api) {
        validateBase(api);
        checkPathUnique(api.getProjectId(), api.getApiPath(), null);
        api.setId(IdUtil.uuid());
        api.setStatus(Api.STATUS_DRAFT);
        api.setVersion("v1");
        apiMapper.insert(api);
    }

    /**
     * 编辑接口。若原状态为「已上线」，编辑后自动回到「草稿」，需重新上线生效（需求 3.3.3）。
     */
    public void update(Api api) {
        Api old = get(api.getId());
        validateBase(api);
        checkPathUnique(api.getProjectId(), api.getApiPath(), api.getId());
        if (old.getStatus() != null && old.getStatus() == Api.STATUS_ONLINE) {
            api.setStatus(Api.STATUS_DRAFT);
        }
        // 清空前端回传的时间戳，避免覆盖数据库真实值（否则 Executor 缓存无法感知变更）
        api.setCreatedAt(null);
        api.setUpdatedAt(null);
        apiMapper.updateById(api);
    }

    /** 状态流转：草稿/下线 → 上线；上线 → 下线 */
    public void updateStatus(String id, Integer target) {
        Api api = get(id);
        if (target == null) {
            throw new BizException("目标状态不能为空");
        }
        if (target == Api.STATUS_ONLINE) {
            // 上线校验
            if (!StringUtils.hasText(api.getSqlContent())) {
                throw new BizException("SQL 内容不能为空，无法上线");
            }
            // 防注入：禁止 ${} 字符串拼接（需求 4.5.1）
            if (api.getSqlContent().contains("${")) {
                throw new BizException("SQL 中不允许使用 ${} 拼接（存在注入风险），请使用 #{} 参数化");
            }
            Project project = projectMapper.selectById(api.getProjectId());
            if (project == null || project.getStatus() == null || project.getStatus() == 0) {
                throw new BizException("所属项目未启用或不存在，无法上线");
            }
            if (api.getStatus() != null && api.getStatus() == Api.STATUS_ONLINE) {
                return;
            }
            if (api.getStatus() != null && api.getStatus() != Api.STATUS_DRAFT && api.getStatus() != Api.STATUS_OFFLINE) {
                throw new BizException("非法状态，无法上线");
            }
            Api patch = new Api();
            patch.setId(id);
            patch.setStatus(Api.STATUS_ONLINE);
            // 版本号由用户在编辑时手工维护，上线不再自动递增
            apiMapper.updateById(patch);
        } else if (target == Api.STATUS_OFFLINE) {
            if (api.getStatus() == null || api.getStatus() != Api.STATUS_ONLINE) {
                throw new BizException("仅已上线接口可下线");
            }
            Api patch = new Api();
            patch.setId(id);
            patch.setStatus(Api.STATUS_OFFLINE);
            apiMapper.updateById(patch);
        } else {
            throw new BizException("非法目标状态");
        }
    }

    /** 删除：仅草稿/下线状态可删除 */
    public void delete(String id) {
        Api api = get(id);
        if (api.getStatus() != null && api.getStatus() == Api.STATUS_ONLINE) {
            throw new BizException("已上线接口不能删除，请先下线");
        }
        apiMapper.deleteById(id);
    }

    private void validateBase(Api api) {
        if (!StringUtils.hasText(api.getProjectId())) {
            throw new BizException("所属项目不能为空");
        }
        if (projectMapper.selectById(api.getProjectId()) == null) {
            throw new BizException("所属项目不存在");
        }
        if (!StringUtils.hasText(api.getApiName())) {
            throw new BizException("接口名称不能为空");
        }
        if (!StringUtils.hasText(api.getApiPath()) || !api.getApiPath().startsWith("/")) {
            throw new BizException("接口路径不能为空且必须以 / 开头");
        }
        if (!StringUtils.hasText(api.getMethod()) || !METHODS.contains(api.getMethod().toUpperCase())) {
            throw new BizException("请求方法必须为 GET/POST/PUT/DELETE");
        }
        api.setMethod(api.getMethod().toUpperCase());
        if (!StringUtils.hasText(api.getSqlType())) {
            api.setSqlType("sql");
        }
        if (!"sql".equals(api.getSqlType()) && !"groovy".equals(api.getSqlType())) {
            throw new BizException("脚本类型必须为 sql 或 groovy");
        }
        if (!StringUtils.hasText(api.getVersion())) {
            api.setVersion("v1");
        }
        if (api.getLogEnabled() == null) {
            api.setLogEnabled(0);
        }
        if (api.getRateLimitQps() == null) {
            api.setRateLimitQps(java.math.BigDecimal.ZERO);
        }
    }

    private void checkPathUnique(String projectId, String apiPath, String excludeId) {
        Long count = apiMapper.selectCount(new LambdaQueryWrapper<Api>()
                .eq(Api::getProjectId, projectId)
                .eq(Api::getApiPath, apiPath)
                .ne(StringUtils.hasText(excludeId), Api::getId, excludeId));
        if (count != null && count > 0) {
            throw new BizException("该项目下已存在路径：" + apiPath);
        }
    }
}
