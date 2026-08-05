package com.lingqu.manager.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lingqu.manager.common.Dialect;
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
 * 系统概览统计。普通用户仅统计自己有权限的项目。
 */
@Service
public class DashboardService {

    private final ProjectMapper projectMapper;
    private final ApiMapper apiMapper;
    private final DatasourceMapper datasourceMapper;
    private final ApiLogMapper apiLogMapper;
    private final PermService permService;

    public DashboardService(ProjectMapper projectMapper, ApiMapper apiMapper,
                            DatasourceMapper datasourceMapper, ApiLogMapper apiLogMapper,
                            PermService permService) {
        this.projectMapper = projectMapper;
        this.apiMapper = apiMapper;
        this.datasourceMapper = datasourceMapper;
        this.apiLogMapper = apiLogMapper;
        this.permService = permService;
    }

    public Map<String, Object> stats() {
        List<String> permitted = permService.permittedProjectIds();
        boolean limited = permitted != null;

        Map<String, Object> result = new HashMap<>();

        // 项目维度
        if (limited) {
            result.put("projectTotal", projectMapper.selectCount(new LambdaQueryWrapper<Project>().in(Project::getId, permitted)));
            result.put("projectOnline", projectMapper.selectCount(new LambdaQueryWrapper<Project>()
                    .in(Project::getId, permitted).eq(Project::getStatus, 1)));
            result.put("apiTotal", apiMapper.selectCount(new LambdaQueryWrapper<Api>().in(Api::getProjectId, permitted)));
            result.put("apiOnline", apiMapper.selectCount(new LambdaQueryWrapper<Api>()
                    .in(Api::getProjectId, permitted).eq(Api::getStatus, Api.STATUS_ONLINE)));
        } else {
            result.put("projectTotal", projectMapper.selectCount(null));
            result.put("projectOnline", projectMapper.selectCount(new LambdaQueryWrapper<Project>().eq(Project::getStatus, 1)));
            result.put("apiTotal", apiMapper.selectCount(null));
            result.put("apiOnline", apiMapper.selectCount(new LambdaQueryWrapper<Api>().eq(Api::getStatus, Api.STATUS_ONLINE)));
        }
        // 数据源管理仅管理员，普通用户统计不展示
        result.put("datasourceTotal", limited ? 0 : datasourceMapper.selectCount(null));

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        if (limited) {
            result.put("logToday", apiLogMapper.selectCount(new LambdaQueryWrapper<ApiLog>()
                    .in(ApiLog::getProjectId, permitted).ge(ApiLog::getCreatedAt, todayStart)));
        } else {
            result.put("logToday", apiLogMapper.selectCount(new LambdaQueryWrapper<ApiLog>()
                    .ge(ApiLog::getCreatedAt, todayStart)));
        }

        // 项目调用量排行 TOP5
        QueryWrapper<ApiLog> qw = new QueryWrapper<>();
        qw.select("COALESCE(project_code, '-') AS project_code", "COUNT(*) AS cnt");
        if (limited) {
            qw.in("project_id", permitted);
        }
        qw.groupBy("project_code").orderByDesc("cnt").last("LIMIT 5");
        result.put("topProjects", apiLogMapper.selectMaps(qw));

        // 数据源健康状态（仅管理员）
        if (limited) {
            result.put("datasourceHealthy", 0);
        } else {
            List<Datasource> datasources = datasourceMapper.selectList(null);
            result.put("datasourceHealthy", (int) datasources.stream().filter(d -> d.getStatus() != null && d.getStatus() == 1).count());
        }

        // 近 7 日调用趋势（按天聚合，方言差异处理）
        String dateExpr = Dialect.isPostgresql() ? "CAST(created_at AS DATE)" : "DATE(created_at)";
        QueryWrapper<ApiLog> trendQw = new QueryWrapper<>();
        trendQw.select(dateExpr + " AS day", "COUNT(*) AS cnt")
                .ge("created_at", LocalDate.now().minusDays(6).atStartOfDay());
        if (limited) {
            trendQw.in("project_id", permitted);
        }
        // PG 不允许 GROUP BY 引用输出列别名，必须用表达式本身
        trendQw.groupBy(dateExpr).orderByAsc("day");
        result.put("trend", apiLogMapper.selectMaps(trendQw));

        // 接口调用量 TOP5
        QueryWrapper<ApiLog> topApiQw = new QueryWrapper<>();
        topApiQw.select("COALESCE(api_name, '-') AS api_name", "COUNT(*) AS cnt");
        if (limited) {
            topApiQw.in("project_id", permitted);
        }
        topApiQw.groupBy("api_name").orderByDesc("cnt").last("LIMIT 5");
        result.put("topApis", apiLogMapper.selectMaps(topApiQw));

        // 今日错误率
        QueryWrapper<ApiLog> errQw = new QueryWrapper<>();
        errQw.ge("created_at", todayStart).and(w -> w.lt("status_code", 200).or().ge("status_code", 300));
        if (limited) {
            errQw.in("project_id", permitted);
        }
        Long todayErrors = apiLogMapper.selectCount(errQw);
        long todayTotalCount = ((Number) result.get("logToday")).longValue();
        result.put("todayErrorRate", todayTotalCount == 0 ? 0 : Math.round(todayErrors * 10000.0 / todayTotalCount) / 100.0);
        return result;
    }
}
