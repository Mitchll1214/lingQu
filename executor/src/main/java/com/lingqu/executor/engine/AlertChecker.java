package com.lingqu.executor.engine;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lingqu.executor.entity.AlertConfig;
import com.lingqu.executor.entity.ApiLog;
import com.lingqu.executor.mapper.AlertConfigMapper;
import com.lingqu.executor.mapper.ApiLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 告警检测器（需求 3.8.3）。
 * 定时分析调用日志窗口，按规则检测：
 *   - timeout：接口平均耗时超过阈值（秒）→ 告警
 *   - error_rate：接口错误率超过阈值（百分比）→ 告警
 * 同一规则在静默期内只发一次（last_alert_at 防抖）。
 */
@Component
public class AlertChecker {

    private static final Logger log = LoggerFactory.getLogger(AlertChecker.class);

    private final AlertConfigMapper alertConfigMapper;
    private final ApiLogMapper apiLogMapper;
    private final AlertMailer alertMailer;

    public AlertChecker(AlertConfigMapper alertConfigMapper, ApiLogMapper apiLogMapper, AlertMailer alertMailer) {
        this.alertConfigMapper = alertConfigMapper;
        this.apiLogMapper = apiLogMapper;
        this.alertMailer = alertMailer;
    }

    @Scheduled(fixedDelayString = "${app.alert-check-seconds:60}000")
    public void check() {
        List<AlertConfig> rules;
        try {
            rules = alertConfigMapper.selectList(new LambdaQueryWrapper<AlertConfig>()
                    .eq(AlertConfig::getStatus, 1));
        } catch (Exception e) {
            log.warn("加载告警规则失败：{}", e.getMessage());
            return;
        }
        if (rules == null || rules.isEmpty()) {
            return;
        }
        for (AlertConfig rule : rules) {
            try {
                checkRule(rule);
            } catch (Exception e) {
                log.warn("告警规则「{}」检测失败：{}", rule.getName(), e.getMessage());
            }
        }
    }

    private void checkRule(AlertConfig rule) {
        // 防抖：静默期内不再重复告警
        if (rule.getLastAlertAt() != null
                && rule.getLastAlertAt().plusMinutes(silence(rule)).isAfter(LocalDateTime.now())) {
            return;
        }
        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(window(rule));

        QueryWrapper<ApiLog> qw = new QueryWrapper<>();
        qw.select("api_id", "api_name",
                "AVG(cost_time) AS avg_cost",
                "COUNT(*) AS total",
                "SUM(CASE WHEN status_code < 200 OR status_code >= 300 THEN 1 ELSE 0 END) AS errors");
        if (StringUtils.hasText(rule.getProjectId())) {
            qw.eq("project_id", rule.getProjectId());
        }
        qw.ge("created_at", windowStart);
        qw.groupBy("api_id", "api_name");

        List<Map<String, Object>> stats = apiLogMapper.selectMaps(qw);
        if (stats == null || stats.isEmpty()) {
            return;
        }

        boolean timeoutType = AlertConfig.TYPE_TIMEOUT.equals(rule.getAlertType());
        double threshold = rule.getThreshold() == null ? 0 : rule.getThreshold().doubleValue();
        List<Map<String, Object>> hits = new ArrayList<>();
        for (Map<String, Object> row : stats) {
            double total = toDouble(row.get("total"));
            if (total <= 0) {
                continue;
            }
            if (timeoutType) {
                double avgMs = toDouble(row.get("avg_cost"));
                if (threshold > 0 && avgMs > threshold * 1000) {
                    hits.add(row);
                }
            } else {
                double errors = toDouble(row.get("errors"));
                if (threshold > 0 && errors / total * 100 > threshold) {
                    hits.add(row);
                }
            }
        }
        if (hits.isEmpty()) {
            return;
        }

        String subject = "【灵渠告警】" + rule.getName();
        StringBuilder body = new StringBuilder();
        body.append("告警规则：").append(rule.getName()).append("\n");
        body.append("规则类型：").append(timeoutType ? "响应超时" : "错误率")
                .append("，阈值：").append(threshold)
                .append(timeoutType ? " 秒" : "%").append("\n");
        body.append("统计窗口：最近 ").append(window(rule)).append(" 分钟\n\n");
        for (Map<String, Object> hit : hits) {
            body.append("· 接口 [").append(hit.get("api_name")).append("]");
            if (timeoutType) {
                body.append(" 平均耗时 ").append(formatDouble(toDouble(hit.get("avg_cost")) / 1000)).append(" 秒");
            } else {
                double total = toDouble(hit.get("total"));
                double errors = toDouble(hit.get("errors"));
                body.append(" 错误率 ").append(formatDouble(errors / total * 100)).append("%");
            }
            body.append("（总调用 ").append(hit.get("total")).append(" 次）\n");
        }
        body.append("\n请及时处理。");

        boolean sent = alertMailer.send(subject, body.toString(), rule.getMailTo());

        // 仅发送成功才更新防抖时间，避免告警丢失后静默期内无法重试
        if (sent) {
            AlertConfig patch = new AlertConfig();
            patch.setId(rule.getId());
            patch.setLastAlertAt(LocalDateTime.now());
            alertConfigMapper.updateById(patch);
        }
    }

    private int window(AlertConfig rule) {
        return rule.getWindowMinutes() == null || rule.getWindowMinutes() <= 0 ? 5 : rule.getWindowMinutes();
    }

    private int silence(AlertConfig rule) {
        return rule.getSilenceMinutes() == null || rule.getSilenceMinutes() <= 0 ? 10 : rule.getSilenceMinutes();
    }

    private double toDouble(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String formatDouble(double v) {
        return String.format("%.2f", v);
    }
}
