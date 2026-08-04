package com.lingqu.manager.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lingqu.manager.entity.Api;
import com.lingqu.manager.entity.ApiLog;
import com.lingqu.manager.entity.Datasource;
import com.lingqu.manager.entity.Project;
import com.lingqu.manager.mapper.ApiLogMapper;
import com.lingqu.manager.mapper.ApiMapper;
import com.lingqu.manager.mapper.DatasourceMapper;
import com.lingqu.manager.mapper.ProjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统概览统计。
 */
@Service
public class DashboardService {

    private final ProjectMapper projectMapper;
    private final ApiMapper apiMapper;
    private final DatasourceMapper datasourceMapper;
    private final ApiLogMapper apiLogMapper;

    public DashboardService(ProjectMapper projectMapper, ApiMapper apiMapper,
                            DatasourceMapper datasourceMapper, ApiLogMapper apiLogMapper) {
        this.projectMapper = projectMapper;
        this.apiMapper = apiMapper;
        this.datasourceMapper = datasourceMapper;
        this.apiLogMapper = apiLogMapper;
    }

    public Map<String, Object> stats() {
        Map<String, Object> result = new HashMap<>();
        result.put("projectTotal", projectMapper.selectCount(null));
        result.put("projectOnline", projectMapper.selectCount(new LambdaQueryWrapper<Project>().eq(Project::getStatus, 1)));
        result.put("apiTotal", apiMapper.selectCount(null));
        result.put("apiOnline", apiMapper.selectCount(new LambdaQueryWrapper<Api>().eq(Api::getStatus, Api.STATUS_ONLINE)));
        result.put("datasourceTotal", datasourceMapper.selectCount(null));

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        result.put("logToday", apiLogMapper.selectCount(new LambdaQueryWrapper<ApiLog>()
                .ge(ApiLog::getCreatedAt, todayStart)));

        QueryWrapper<ApiLog> qw = new QueryWrapper<>();
        qw.select("COALESCE(project_code, '-') AS project_code", "COUNT(*) AS cnt")
                .groupBy("project_code")
                .orderByDesc("cnt")
                .last("LIMIT 5");
        result.put("topProjects", apiLogMapper.selectMaps(qw));

        // 数据源健康状态
        List<Datasource> datasources = datasourceMapper.selectList(null);
        result.put("datasourceHealthy", (int) datasources.stream().filter(d -> d.getStatus() != null && d.getStatus() == 1).count());
        return result;
    }
}
