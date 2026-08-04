package com.lingqu.executor.engine;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingqu.executor.entity.Api;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 出参映射（需求 3.3.8）：按 response_format 配置对返回结果做字段重命名/格式化。
 * 配置格式（JSON 数组）：
 *   [{"source":"user_name","target":"userName"}, {"source":"created_at","target":"createdAt"}]
 * 配置了映射时，输出仅保留映射中声明的字段（source 原字段名 / target 输出字段名）。
 * format 字段预留（日期格式化等），当前版本仅实现重命名。
 */
@Component
public class ResponseFormatter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * 对 SQL/Groovy 的执行结果应用出参映射。
     * 仅处理 List&lt;Map&gt; 形态的结果；DML 的 affectedRows 等结构不转换。
     */
    public Object format(Api api, Object data) {
        if (data == null || !(data instanceof List)) {
            return data;
        }
        String fmt = api.getResponseFormat();
        if (!StringUtils.hasText(fmt)) {
            return data;
        }
        List<Map<String, Object>> mappings = parse(fmt);
        if (mappings.isEmpty()) {
            return data;
        }
        List<?> rows = (List<?>) data;
        List<Map<String, Object>> result = new ArrayList<>(rows.size());
        for (Object row : rows) {
            if (!(row instanceof Map)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> cast = (Map<String, Object>) row;
                result.add(cast);
                continue;
            }
            Map<?, ?> src = (Map<?, ?>) row;
            Map<String, Object> out = new LinkedHashMap<>();
            for (Map<String, Object> m : mappings) {
                String source = m.get("source") == null ? "" : String.valueOf(m.get("source"));
                String target = m.get("target") == null ? source : String.valueOf(m.get("target"));
                out.put(target, src.get(source));
            }
            result.add(out);
        }
        return result;
    }

    private List<Map<String, Object>> parse(String fmt) {
        try {
            return MAPPER.readValue(fmt, new TypeReference<List<Map<String, Object>>>() {
            });
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
