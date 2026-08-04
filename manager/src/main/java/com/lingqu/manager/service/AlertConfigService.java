package com.lingqu.manager.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lingqu.manager.common.BizException;
import com.lingqu.manager.common.IdUtil;
import com.lingqu.manager.entity.AlertConfig;
import com.lingqu.manager.entity.Project;
import com.lingqu.manager.mapper.AlertConfigMapper;
import com.lingqu.manager.mapper.ProjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 告警规则管理（需求 3.8.3）。
 */
@Service
public class AlertConfigService {

    private final AlertConfigMapper alertConfigMapper;
    private final ProjectMapper projectMapper;

    public AlertConfigService(AlertConfigMapper alertConfigMapper, ProjectMapper projectMapper) {
        this.alertConfigMapper = alertConfigMapper;
        this.projectMapper = projectMapper;
    }

    public IPage<AlertConfig> page(long page, long size, String keyword) {
        LambdaQueryWrapper<AlertConfig> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.like(AlertConfig::getName, keyword);
        }
        qw.orderByDesc(AlertConfig::getUpdatedAt);
        return alertConfigMapper.selectPage(new Page<>(page, size), qw);
    }

    public List<AlertConfig> list() {
        return alertConfigMapper.selectList(new LambdaQueryWrapper<AlertConfig>()
                .orderByDesc(AlertConfig::getUpdatedAt));
    }

    public AlertConfig get(String id) {
        AlertConfig config = alertConfigMapper.selectById(id);
        if (config == null) {
            throw new BizException(404, "告警规则不存在");
        }
        return config;
    }

    public void create(AlertConfig config) {
        validate(config);
        config.setId(IdUtil.uuid());
        if (config.getStatus() == null) {
            config.setStatus(1);
        }
        alertConfigMapper.insert(config);
    }

    public void update(AlertConfig config) {
        get(config.getId());
        validate(config);
        config.setCreatedAt(null);
        config.setUpdatedAt(null);
        alertConfigMapper.updateById(config);
    }

    public void updateStatus(String id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BizException("状态值只能为 0（禁用）或 1（启用）");
        }
        AlertConfig patch = new AlertConfig();
        patch.setId(id);
        patch.setStatus(status);
        int rows = alertConfigMapper.updateById(patch);
        if (rows == 0) {
            throw new BizException(404, "告警规则不存在");
        }
    }

    public void delete(String id) {
        get(id);
        alertConfigMapper.deleteById(id);
    }

    private void validate(AlertConfig config) {
        if (!StringUtils.hasText(config.getName())) {
            throw new BizException("规则名称不能为空");
        }
        if (!AlertConfig.TYPE_TIMEOUT.equals(config.getAlertType())
                && !AlertConfig.TYPE_ERROR_RATE.equals(config.getAlertType())) {
            throw new BizException("规则类型必须为 timeout 或 error_rate");
        }
        if (config.getThreshold() == null || config.getThreshold().doubleValue() <= 0) {
            throw new BizException("阈值必须大于 0");
        }
        if (StringUtils.hasText(config.getProjectId())) {
            Project project = projectMapper.selectById(config.getProjectId());
            if (project == null) {
                throw new BizException("绑定的项目不存在");
            }
        } else {
            config.setProjectId(null);
        }
        if (config.getWindowMinutes() == null || config.getWindowMinutes() <= 0) {
            config.setWindowMinutes(5);
        }
        if (config.getSilenceMinutes() == null || config.getSilenceMinutes() <= 0) {
            config.setSilenceMinutes(10);
        }
    }
}
