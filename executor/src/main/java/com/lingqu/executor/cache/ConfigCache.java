package com.lingqu.executor.cache;

import com.lingqu.executor.entity.Api;
import com.lingqu.executor.entity.Datasource;
import com.lingqu.executor.entity.Project;
import com.lingqu.executor.entity.Token;
import com.lingqu.executor.mapper.ApiMapper;
import com.lingqu.executor.mapper.DatasourceMapper;
import com.lingqu.executor.mapper.ProjectMapper;
import com.lingqu.executor.mapper.TokenMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 项目/接口/Token/数据源配置的内存缓存。
 * 定时从配置库全量刷新（默认 30s），满足「重启后从数据库重新加载、规则不丢失」与
 * 「管理员修改配置后定期生效」的需求（需求 3.6.3 / 3.6.4）。
 */
@Component
public class ConfigCache {

    private static final Logger log = LoggerFactory.getLogger(ConfigCache.class);

    private final ProjectMapper projectMapper;
    private final ApiMapper apiMapper;
    private final TokenMapper tokenMapper;
    private final DatasourceMapper datasourceMapper;

    private volatile Map<String, Project> projectByPrefix = Collections.emptyMap();
    private volatile Map<String, Api> apiByKey = Collections.emptyMap();
    private volatile Map<String, Datasource> datasourceById = Collections.emptyMap();
    private volatile Map<String, List<Token>> tokensByProject = Collections.emptyMap();

    public ConfigCache(ProjectMapper projectMapper, ApiMapper apiMapper,
                       TokenMapper tokenMapper, DatasourceMapper datasourceMapper) {
        this.projectMapper = projectMapper;
        this.apiMapper = apiMapper;
        this.tokenMapper = tokenMapper;
        this.datasourceMapper = datasourceMapper;
    }

    @PostConstruct
    public void init() {
        refresh();
    }

    @Scheduled(fixedDelayString = "${app.config-refresh-seconds:30}000")
    public void refresh() {
        try {
            Map<String, Project> pMap = new HashMap<>();
            for (Project p : projectMapper.selectList(null)) {
                if (p.getDeleted() == null || p.getDeleted() == 0) {
                    pMap.put(p.getRoutePrefix(), p);
                }
            }
            Map<String, Api> aMap = new HashMap<>();
            for (Api a : apiMapper.selectList(null)) {
                aMap.put(a.getProjectId() + "|" + a.getApiPath(), a);
            }
            Map<String, Datasource> dMap = new HashMap<>();
            for (Datasource d : datasourceMapper.selectList(null)) {
                dMap.put(d.getId(), d);
            }
            Map<String, List<Token>> tMap = new HashMap<>();
            for (Token t : tokenMapper.selectList(null)) {
                tMap.computeIfAbsent(t.getProjectId(), k -> new ArrayList<>()).add(t);
            }
            // volatile 一次性发布，读方拿到的是完整一致快照
            this.projectByPrefix = pMap;
            this.apiByKey = aMap;
            this.datasourceById = dMap;
            this.tokensByProject = tMap;
        } catch (Exception e) {
            log.warn("配置刷新失败（保留上次快照）: {}", e.getMessage());
        }
    }

    public Project findProject(String routePrefix) {
        return projectByPrefix.get(routePrefix);
    }

    /** 全部项目（供路由最长前缀匹配） */
    public java.util.Collection<Project> allProjects() {
        return projectByPrefix.values();
    }

    public Api findApi(String projectId, String apiPath) {
        return apiByKey.get(projectId + "|" + apiPath);
    }

    public Datasource getDatasource(String datasourceId) {
        return datasourceById.get(datasourceId);
    }

    public List<Token> getTokens(String projectId) {
        return tokensByProject.getOrDefault(projectId, Collections.emptyList());
    }
}
