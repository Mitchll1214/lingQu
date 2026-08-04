package com.lingqu.executor.engine;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingqu.executor.common.BizException;
import com.lingqu.executor.entity.Api;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
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
     * 仅处理 List 形态的结果；DML 的 affectedRows 等结构不转换。
     * 配置了 response_format 但内容非法时直接报错（避免静默返回全量字段造成数据泄露）。
     */
    public Object format(Api api, Object data) {
        if (data == null || !(data instanceof List)) {
            return data;
        }
        String fmt = api.getResponseFormat();
        if (!StringUtils.hasText(fmt)) {
            return data;
        }
        List<Map<String, Object>> mappings;
        try {
            mappings = MAPPER.readValue(fmt, new TypeReference<List<Map<String, Object>>>() {
            });
        } catch (Exception e) {
            throw new BizException(500, "出参映射配置不是合法 JSON，请检查接口「" + api.getApiName() + "」的 response_format");
        }
        if (mappings == null || mappings.isEmpty()) {
            return data;
        }

        List<?> rows = (List<?>) data;
        List<Object> result = new ArrayList<>(rows.size());
        for (Object row : rows) {
            // 非 Map 行（如标量列表）原样保留
            if (!(row instanceof Map)) {
                result.add(row);
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
}
