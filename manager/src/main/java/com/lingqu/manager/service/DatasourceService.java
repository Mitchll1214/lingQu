package com.lingqu.manager.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lingqu.manager.common.BizException;
import com.lingqu.manager.common.CryptoUtil;
import com.lingqu.manager.common.IdUtil;
import com.lingqu.manager.entity.Datasource;
import com.lingqu.manager.entity.Project;
import com.lingqu.manager.mapper.DatasourceMapper;
import com.lingqu.manager.mapper.ProjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.Properties;

/**
 * 数据源管理。
 */
@Service
public class DatasourceService {

    private final DatasourceMapper datasourceMapper;
    private final ProjectMapper projectMapper;

    public DatasourceService(DatasourceMapper datasourceMapper, ProjectMapper projectMapper) {
        this.datasourceMapper = datasourceMapper;
        this.projectMapper = projectMapper;
    }

    public IPage<Datasource> page(long page, long size, String keyword) {
        LambdaQueryWrapper<Datasource> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.like(Datasource::getName, keyword).or().like(Datasource::getJdbcUrl, keyword);
        }
        qw.orderByDesc(Datasource::getUpdatedAt);
        IPage<Datasource> result = datasourceMapper.selectPage(new Page<>(page, size), qw);
        result.getRecords().forEach(d -> d.setPassword(null));
        return result;
    }

    public Datasource get(String id) {
        Datasource ds = datasourceMapper.selectById(id);
        if (ds == null) {
            throw new BizException(404, "数据源不存在");
        }
        return ds;
    }

    public void create(Datasource ds) {
        validateBase(ds);
        if (!StringUtils.hasText(ds.getPassword())) {
            throw new BizException("密码不能为空");
        }
        ds.setId(IdUtil.uuid());
        if (ds.getStatus() == null) {
            ds.setStatus(1);
        }
        ds.setPassword(CryptoUtil.encrypt(ds.getPassword()));
        datasourceMapper.insert(ds);
    }

    public void update(Datasource ds) {
        get(ds.getId());
        validateBase(ds);
        // 密码留空表示不修改
        if (StringUtils.hasText(ds.getPassword())) {
            ds.setPassword(CryptoUtil.encrypt(ds.getPassword()));
        } else {
            ds.setPassword(null);
        }
        // 清空前端回传的时间戳，避免覆盖数据库真实值（否则 Executor 连接池无法感知变更）
        ds.setCreatedAt(null);
        ds.setUpdatedAt(null);
        datasourceMapper.updateById(ds);
    }

    /** 删除：校验无项目引用 */
    public void delete(String id) {
        get(id);
        Long refCount = projectMapper.selectCount(new LambdaQueryWrapper<Project>()
                .eq(Project::getDatasourceId, id));
        if (refCount != null && refCount > 0) {
            throw new BizException("仍有项目绑定该数据源，无法删除");
        }
        datasourceMapper.deleteById(id);
    }

    /** 连接测试：返回连接信息 */
    public String test(String id) {
        Datasource ds = get(id);
        String plainPassword = CryptoUtil.decrypt(ds.getPassword());
        String driverClass = resolveDriver(ds);
        try {
            Class.forName(driverClass);
            DriverManager.setLoginTimeout(3);
            Properties props = new Properties();
            props.put("user", ds.getUsername() == null ? "" : ds.getUsername());
            props.put("password", plainPassword == null ? "" : plainPassword);
            try (Connection conn = DriverManager.getConnection(ds.getJdbcUrl(), props)) {
                return "连接成功：" + conn.getMetaData().getDatabaseProductName()
                        + " " + conn.getMetaData().getDatabaseProductVersion();
            }
        } catch (Exception e) {
            throw new BizException("连接失败：" + e.getMessage());
        }
    }

    public List<Datasource> options() {
        List<Datasource> list = datasourceMapper.selectList(new LambdaQueryWrapper<Datasource>()
                .orderByAsc(Datasource::getName));
        list.forEach(d -> d.setPassword(null));
        return list;
    }

    private void validateBase(Datasource ds) {
        if (!StringUtils.hasText(ds.getName())) {
            throw new BizException("数据源名称不能为空");
        }
        if (!StringUtils.hasText(ds.getDbType())) {
            throw new BizException("数据库类型不能为空");
        }
        if (!StringUtils.hasText(ds.getJdbcUrl())) {
            throw new BizException("JDBC URL 不能为空");
        }
        // 自动识别驱动类名
        String resolved = resolveDriver(ds);
        if (resolved == null) {
            if (!StringUtils.hasText(ds.getDriverClass())) {
                throw new BizException("该数据库类型未内置驱动，请填写驱动类名");
            }
        } else {
            ds.setDriverClass(resolved);
        }
    }

    /** 内置驱动识别；不支持的返回 null（由用户填写 driver_class） */
    private String resolveDriver(Datasource ds) {
        String type = ds.getDbType() == null ? "" : ds.getDbType().trim().toLowerCase();
        switch (type) {
            case "mysql":
                return "com.mysql.cj.jdbc.Driver";
            case "postgresql":
            case "pg":
            case "postgres":
                return "org.postgresql.Driver";
            default:
                return null;
        }
    }
}
