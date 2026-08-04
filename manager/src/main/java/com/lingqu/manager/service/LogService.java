package com.lingqu.manager.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lingqu.manager.entity.ApiLog;
import com.lingqu.manager.mapper.ApiLogMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 调用日志查询。
 */
@Service
public class LogService {

    private final ApiLogMapper apiLogMapper;

    public LogService(ApiLogMapper apiLogMapper) {
        this.apiLogMapper = apiLogMapper;
    }

    public IPage<ApiLog> page(long page, long size, String projectId, String apiId,
                              LocalDateTime start, LocalDateTime end, Integer success) {
        LambdaQueryWrapper<ApiLog> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(projectId)) {
            qw.eq(ApiLog::getProjectId, projectId);
        }
        if (StringUtils.hasText(apiId)) {
            qw.eq(ApiLog::getApiId, apiId);
        }
        if (start != null) {
            qw.ge(ApiLog::getCreatedAt, start);
        }
        if (end != null) {
            qw.le(ApiLog::getCreatedAt, end);
        }
        if (success != null) {
            // 成功：2xx；失败：其他
            if (success == 1) {
                qw.ge(ApiLog::getStatusCode, 200).lt(ApiLog::getStatusCode, 300);
            } else {
                qw.and(w -> w.lt(ApiLog::getStatusCode, 200).or().ge(ApiLog::getStatusCode, 300));
            }
        }
        qw.orderByDesc(ApiLog::getCreatedAt);
        return apiLogMapper.selectPage(new Page<>(page, size), qw);
    }
}
