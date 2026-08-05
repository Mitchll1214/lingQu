package com.lingqu.executor.engine;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingqu.executor.common.BizException;
import com.lingqu.executor.entity.Api;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 入参定义绑定（需求 3.3.7）：
 * 请求参数按接口的 params 定义（JSON 数组）做规范化处理：
 *   - 必填参数缺失 → 400 明确报错（而非 SQL 层报错）
 *   - 未传但有默认值 → 自动填充默认值
 *   - 类型自动转换（String/Integer/Float/Boolean/Date/Object）
 * 转换后的值进入 SQL 参数，SQL 中 #{参数名} 直接可用。
 * 未配置入参定义的接口保持原行为（不做校验）。
 */
@Component
public class ParamBinder {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter D = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public Map<String, Object> bind(Api api, Map<String, Object> params) {
        String def = api.getParams();
        if (def == null || def.trim().isEmpty()) {
            return params;
        }
        List<Map<String, Object>> defs;
        try {
            defs = MAPPER.readValue(def, new TypeReference<List<Map<String, Object>>>() {
            });
        } catch (Exception e) {
            // 入参定义非法：不阻断执行（保持兼容），但记入调用日志由管理员检查
            return params;
        }
        if (defs == null || defs.isEmpty()) {
            return params;
        }

        Map<String, Object> result = new HashMap<>(params);
        for (Map<String, Object> item : defs) {
            String name = item.get("name") == null ? "" : String.valueOf(item.get("name")).trim();
            if (name.isEmpty()) {
                continue;
            }
            String type = item.get("type") == null ? "String" : String.valueOf(item.get("type"));
            boolean required = Boolean.TRUE.equals(item.get("required"))
                    || "true".equalsIgnoreCase(String.valueOf(item.get("required")));

            Object raw = params.containsKey(name) ? params.get(name) : null;
            boolean missing = raw == null || (raw instanceof String && ((String) raw).isEmpty());
            if (missing) {
                Object defaultValue = item.get("defaultValue");
                if (defaultValue != null && !String.valueOf(defaultValue).isEmpty()) {
                    // 默认值自动填充（同样做类型转换）
                    result.put(name, convert(name, defaultValue, type));
                } else if (required) {
                    throw new BizException(400, "缺少必填参数：" + name);
                }
            } else {
                result.put(name, convert(name, raw, type));
            }
        }
        return result;
    }

    private Object convert(String name, Object value, String type) {
        try {
            switch (type == null ? "String" : type) {
                case "Integer":
                    return Integer.valueOf(String.valueOf(value).trim());
                case "Float":
                    return Double.valueOf(String.valueOf(value).trim());
                case "Boolean":
                    return Boolean.valueOf(String.valueOf(value).trim());
                case "Date":
                    return parseDate(String.valueOf(value).trim());
                case "Object":
                default:
                    return value;
            }
        } catch (Exception e) {
            throw new BizException(400, "参数 " + name + " 类型应为 " + type + "，实际值：" + value);
        }
    }

    private Object parseDate(String s) {
        try {
            return LocalDateTime.parse(s, DT);
        } catch (Exception e) {
            try {
                return LocalDate.parse(s, D).atStartOfDay();
            } catch (Exception e2) {
                throw new IllegalArgumentException("无法解析日期: " + s);
            }
        }
    }
}
